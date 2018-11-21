package com.yyz.hover;


import android.os.Message;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.yyz.hover.entity.HoverEntityImageSize;
import com.yyz.hover.entity.HoverLoadImageEntity;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;

import storage.FileHelper;
import task.executor.BaseLoopTask;
import task.executor.TaskContainer;
import task.executor.joggle.ILoopTaskExecutor;
import util.IoUtils;

public class HoverImageLoadTask extends BaseLoopTask {

    private ILoopTaskExecutor mExecutor;

    private int timeout = 3000;


    public static final String TAG_HTTP = "http";
    public static final String TAG_FILE = "file://";


    protected HoverImageLoadTask() {
        TaskContainer container = new TaskContainer(this);
        mExecutor = container.getTaskExecutor();
    }

    public ILoopTaskExecutor getExecutor() {
        return mExecutor;
    }


    //---------------------------------------------------------------------------------


    public void setTimeout(int timeout) {
        if (timeout > 0) {
            this.timeout = timeout;
        }
    }

    public byte[] downloadSyncImage(String path) {
        HttpURLConnection connection = null;
        byte[] data;
        try {
            URL url = new URL(path);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            InputStream is = url.openStream();
            data = IoUtils.tryRead(is);
        } catch (Throwable e) {
            data = null;
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return data;
    }

    //---------------------------------------------------------------------------------

    private void downloadImage(HoverLoadImageEntity entity) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(entity.path);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Connection", "close");
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            InputStream is = connection.getInputStream();
            entity.imageData = IoUtils.tryRead(is);
        } catch (Throwable e) {
            entity.imageData = null;
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void loadLocalFile(HoverLoadImageEntity entity, HoverEntityImageSize imageSize) {
        if (entity.path.length() < 8) {
            return;
        }
        String filePath = entity.path.substring(7);
        File file = new File(filePath);
        if (file.exists()) {
            try {
                entity.image = HoverBitmapHelper.decodeBitmap(filePath, imageSize.width, imageSize.height);
            } catch (Throwable e) {
                entity.imageData = null;
                e.printStackTrace();
            }
        }
    }

    private void sendHandlerMsg(HoverLoadImageEntity entity) {
        Message msg = Message.obtain();
        msg.obj = entity;
        HoverUIRefreshHandler.getInstance().sendMessage(msg);
    }


    @Override
    protected void onRunLoopTask() {
        HoverLoadImageEntity entity = HoverTaskAllocationManger.getInstance().getNewTask();
        if (entity == null) {
            mExecutor.waitTask(0);
            return;
        }

        String key = null;
        if (entity.path != null) {
            key = HoverCacheManger.getInstance().getUrlBase64(entity.path.getBytes());
        }
        if (key == null && entity.imageData == null) {
            //显示错误rid图片
            sendHandlerMsg(entity);
            return;
        }

        //如果没有控件则只进行下载
        if (entity.view == null) {
            downloadImage(entity);
            HoverCacheManger.getInstance().saveImageToFile(key, entity.imageData);
            sendHandlerMsg(entity);
            return;
        }

        //获取控件的大小
        HoverEntityImageSize imageSize = getImageViewWidth(entity.view);
        if (entity.loadPolicy == null && key != null) {
            entity.loadPolicy = HoverLoadPolicy.CACHE_OR_NET;
        }

        if (entity.loadPolicy != HoverLoadPolicy.ONLY_NET) {
            //获取缓存数据
            entity.image = HoverCacheManger.getInstance().getBitmapFromCache(key);
            if (entity.image == null) {
                String path = HoverCacheManger.getInstance().getBitmapFromFile(key);
                if (path != null) {
                    entity.image = HoverBitmapHelper.decodeBitmap(path, imageSize.width, imageSize.height);
                    if (entity.image != null) {
                        HoverCacheManger.getInstance().addBitmapToCache(key, entity.image);
                    }
                }
            }
            if (entity.image != null) {
                sendHandlerMsg(entity);
            }
            if (HoverLoadPolicy.CACHE_OR_NET == entity.loadPolicy && entity.image != null
                    || HoverLoadPolicy.ONLY_CACHE == entity.loadPolicy) {
                return;
            }
        }

        if (entity.path != null && entity.path.startsWith(TAG_HTTP)) {
            downloadImage(entity);
            //如果下载图片失败
            if (entity.imageData == null) {
                sendHandlerMsg(entity);
                return;
            }
            entity.image = HoverBitmapHelper.decodeBitmap(entity.imageData, imageSize.width, imageSize.height);

            if (entity.handleListener != null) {
                entity.image = entity.handleListener.onHandle(entity.view, entity.image);
            }

            if (entity.loadPolicy != HoverLoadPolicy.ONLY_NET) {
                //保存到缓存中
                HoverCacheManger.getInstance().addBitmapToCache(key, entity.image);

                //持久化保存
                HoverCacheManger.getInstance().saveImageToFile(key, entity.imageData);
            }

        } else if (entity.path != null && entity.path.startsWith(TAG_FILE)) {
            loadLocalFile(entity, imageSize);

            if (entity.handleListener != null) {
                entity.image = entity.handleListener.onHandle(entity.view, entity.image);
            }
            //保存到缓存中
            HoverCacheManger.getInstance().addBitmapToCache(key, entity.image);
        } else if (entity.imageData != null) {
            //显示byte[] 图片
            entity.image = HoverBitmapHelper.decodeBitmap(entity.imageData, imageSize.width, imageSize.height);
            if (entity.handleListener != null) {
                entity.image = entity.handleListener.onHandle(entity.view, entity.image);
            }
        }
        sendHandlerMsg(entity);
    }


    /**
     * 根据ImageView获得适当的压缩的宽和高
     *
     * @param imageView
     * @return
     */
    private HoverEntityImageSize getImageViewWidth(ImageView imageView) {
        HoverEntityImageSize imageSize = new HoverEntityImageSize();
        DisplayMetrics displayMetrics = imageView.getContext().getResources().getDisplayMetrics();
        ViewGroup.LayoutParams params = imageView.getLayoutParams();

        // Get actual image width
        int width = params.width == ViewGroup.LayoutParams.WRAP_CONTENT ? 0 : imageView.getWidth();
        if (width <= 0) {
            // Get layout width parameter
            width = params.width;
        }
        if (width <= 0) {
            width = getImageViewFieldValue(imageView, "mMaxWidth");
        }
        // maxWidth
        // parameter
        if (width <= 0) {
            width = displayMetrics.widthPixels;
        }
        // Get actual image height
        int height = params.height == ViewGroup.LayoutParams.WRAP_CONTENT ? 0 : imageView.getHeight();
        if (height <= 0) {
            // Get layout height parameter
            height = params.height;
        }
        if (height <= 0) {
            height = getImageViewFieldValue(imageView, "mMaxHeight");
        }
        // maxHeight
        // parameter
        if (height <= 0) {
            height = displayMetrics.heightPixels;
        }
        imageSize.width = width;
        imageSize.height = height;
        return imageSize;
    }

    /**
     * 反射获得ImageView设置的最大宽度和高度
     *
     * @param object
     * @param fieldName
     * @return
     */
    private int getImageViewFieldValue(Object object, String fieldName) {
        int value = 0;
        try {
            Field field = ImageView.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            int fieldValue = (Integer) field.get(object);
            if (fieldValue > 0 && fieldValue < Integer.MAX_VALUE) {
                value = fieldValue;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

}
