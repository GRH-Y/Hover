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
import java.util.Arrays;

import task.executor.BaseLoopTask;
import task.executor.TaskContainer;
import task.executor.joggle.ILoopTaskExecutor;
import util.IoUtils;
import util.SpeedReflex;

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


    public byte[] downloadImage(String path) {
        HttpURLConnection connection = null;
        byte[] data = null;
        try {
            URL url = new URL(path);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            InputStream is = connection.getInputStream();
            data = IoUtils.tryRead(is);
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return data;
    }

    //---------------------------------------------------------------------------------

    private void loadLocalFile(HoverLoadImageEntity entity, HoverEntityImageSize imageSize) {
        if (entity.path.length() < 8) {
            return;
        }
        String filePath = entity.path.substring(7);
        File file = new File(filePath);
        if (file.exists()) {
            try {
                entity.bitmap = HoverBitmapHelper.decodeBitmap(filePath, imageSize.width, imageSize.height);
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
            entity.imageData = downloadImage(entity.path);
            HoverCacheManger.getInstance().saveImageToFile(key, entity.imageData);
            sendHandlerMsg(entity);
            return;
        }

        //获取控件的大小
        HoverEntityImageSize imageSize = getImageViewWidth(entity.view);
        if (entity.loadPolicy == null && key != null) {
            entity.loadPolicy = HoverLoadPolicy.ALL;
        }

        if (entity.loadPolicy != HoverLoadPolicy.ONLY_NET) {
            //获取缓存数据
            entity.bitmap = HoverCacheManger.getInstance().getBitmapFromCache(key);
            entity.imageData = HoverCacheManger.getInstance().getBitmapForByte(key);
            if (entity.bitmap == null) {
                String path = HoverCacheManger.getInstance().getBitmapFromFile(key);
                if (path != null) {
                    entity.bitmap = HoverBitmapHelper.decodeBitmap(path, imageSize.width, imageSize.height);
                }
            }
            if (entity.bitmap != null) {
                sendHandlerMsg(entity);
            }
            if (HoverLoadPolicy.CACHE_OR_NET == entity.loadPolicy && entity.bitmap != null
                    || HoverLoadPolicy.ONLY_CACHE == entity.loadPolicy) {
                return;
            }
        }

        boolean isNeedSendMeg = true;
        if (entity.path != null && entity.path.startsWith(TAG_HTTP)) {
            byte[] downloadData = downloadImage(entity.path);
            //如果下载图片失败
            if (downloadData == null) {
                sendHandlerMsg(entity);
                return;
            }
            if (entity.imageData != null && Arrays.equals(downloadData, entity.imageData)) {
                //如果图片没有变化则不需要再更新
                isNeedSendMeg = false;
            } else {
                entity.imageData = downloadData;
                entity.bitmap = HoverBitmapHelper.decodeBitmap(entity.imageData, imageSize.width, imageSize.height);
                if (entity.handleListener != null) {
                    entity.bitmap = entity.handleListener.onHandle(entity.view, entity.bitmap);
                }
                if (entity.loadPolicy != HoverLoadPolicy.ONLY_NET) {
                    //保存到缓存中
                    HoverCacheManger.getInstance().addBitmapToCache(key, entity.bitmap);
                    //持久化保存
                    HoverCacheManger.getInstance().saveImageToFile(key, entity.imageData);
                }
            }
        } else if (entity.path != null && entity.path.startsWith(TAG_FILE)) {
            loadLocalFile(entity, imageSize);
            if (entity.handleListener != null) {
                entity.bitmap = entity.handleListener.onHandle(entity.view, entity.bitmap);
            }
            //保存到缓存中
            HoverCacheManger.getInstance().addBitmapToCache(key, entity.bitmap);
        } else if (entity.imageData != null) {
            //显示byte[] 图片
            entity.bitmap = HoverBitmapHelper.decodeBitmap(entity.imageData, imageSize.width, imageSize.height);
            if (entity.handleListener != null) {
                entity.bitmap = entity.handleListener.onHandle(entity.view, entity.bitmap);
            }
        }
        if (isNeedSendMeg) {
            sendHandlerMsg(entity);
        }
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

        // Get actual bitmap width
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
        // Get actual bitmap height
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
            SpeedReflex speedReflex = SpeedReflex.getCache();
            Field field = speedReflex.getField(ImageView.class, fieldName);
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
