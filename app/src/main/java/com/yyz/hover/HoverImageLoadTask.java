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

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;

import task.executor.BaseLoopTask;
import task.executor.TaskContainer;
import task.executor.joggle.ILoopTaskExecutor;
import util.IoEnvoy;
import util.SpeedReflex;
import util.StringEnvoy;

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
            if (path.startsWith("https")) {
                HttpsURLConnection httpsURLConnection = (HttpsURLConnection) url.openConnection();
                httpsURLConnection.setHostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                });
                httpsURLConnection.setSSLSocketFactory((SSLSocketFactory) SSLSocketFactory.getDefault());
                connection = httpsURLConnection;
            } else {
                connection = (HttpURLConnection) url.openConnection();
            }
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setRequestMethod("GET");
            connection.setUseCaches(false);
            connection.setRequestProperty("User-Agent", "Hover");
            //此处为暴力方法设置接受所有类型，以此来防范返回415;
            connection.setRequestProperty("Accept", "*/*");
            connection.setRequestProperty("Accept-Encoding", "gzip, deflate");

            String redirect = connection.getHeaderField("Location");
            if (redirect != null) {
                data = downloadImage(redirect);
            } else {
                int length = connection.getContentLength();
                InputStream is = connection.getInputStream();
                if (length > 0) {
                    data = new byte[length];
                    IoEnvoy.readToFull(is, data);
                } else {
                    data = IoEnvoy.tryRead(is);
                }
            }
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
                if (e instanceof OutOfMemoryError) {
                    HoverCacheManger.getInstance().clearCacheImage();
                }
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
        if (StringEnvoy.isNotEmpty(entity.path)) {
            key = HoverCacheManger.getInstance().getUrlConvertKey(entity.path.getBytes());
        }
        if (key == null && entity.imageData == null) {
            //显示错误rid图片
            sendHandlerMsg(entity);
            return;
        }

        if (entity.loadPolicy == null && key != null) {
            entity.loadPolicy = HoverLoadPolicy.ALL;
        }

        //如果没有控件则只进行下载
        if (entity.view == null) {
            //获取缓存的图片
            entity.imageData = HoverCacheManger.getInstance().getBitmapForByte(key);
            if (entity.imageData != null) {
                sendHandlerMsg(entity);
            }
            //根据策略来决定是否请求网络
            if (HoverLoadPolicy.ONLY_CACHE != entity.loadPolicy && entity.path.startsWith(TAG_HTTP)) {
                //下载图片
                byte[] download = downloadImage(entity.path);
                if (entity.imageData != null && download != null && entity.imageData.length == download.length) {
                    //如果下载的图片数据跟缓存的数据大小一致则不需要重新更新缓存和回调
                    return;
                }
                entity.imageData = download;
                if (entity.loadPolicy != HoverLoadPolicy.ONLY_NET) {
                    HoverCacheManger.getInstance().saveImageToFile(key, entity.imageData);
                }
                sendHandlerMsg(entity);
            }
            return;
        }

        //获取控件的大小
        HoverEntityImageSize imageSize = getImageViewWidth(entity.view);

        boolean isNeedSendMeg = true;
        if (entity.path != null && entity.path.startsWith(TAG_HTTP)) {
            if (entity.loadPolicy != HoverLoadPolicy.ONLY_NET) {
                //获取缓存数据
                entity.bitmap = HoverCacheManger.getInstance().getBitmapFromCache(key);
                if (entity.bitmap == null) {
                    String path = HoverCacheManger.getInstance().getBitmapFromFile(key);
                    if (path != null) {
                        try {
                            entity.bitmap = HoverBitmapHelper.decodeBitmap(path, imageSize.width, imageSize.height);
                            if (entity.bitmap != null) {
                                HoverCacheManger.getInstance().addBitmapToCache(key, entity.bitmap);
                            }
                        } catch (Throwable e) {
                            if (e instanceof OutOfMemoryError) {
                                HoverCacheManger.getInstance().clearCacheImage();
                            }
                            e.printStackTrace();
                        }
                    }
                }
                if ((HoverLoadPolicy.CACHE_OR_NET == entity.loadPolicy && entity.bitmap != null) || HoverLoadPolicy.ONLY_CACHE == entity.loadPolicy) {
                    //CACHE_OR_NET 缓存有则不再请求数据，ONLY_CACHE 只读缓存数据
                    sendHandlerMsg(entity);
                    return;
                }
                if (entity.bitmap != null) {
                    sendHandlerMsg(entity);
                }
            }

            byte[] downloadData = downloadImage(entity.path);
            //如果下载图片失败
            if (downloadData == null) {
                sendHandlerMsg(entity);
                return;
            }
            long fileLength = 0;
            try {
                String cacheFile = HoverCacheManger.getInstance().getBitmapFromFile(key);
                if (StringEnvoy.isNotEmpty(cacheFile)) {
                    File file = new File(cacheFile);
                    fileLength = file.length();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (downloadData.length == fileLength) {
                //如果图片没有变化则不需要再更新
                isNeedSendMeg = false;
                //保存到缓存中
                HoverCacheManger.getInstance().addBitmapToCache(key, entity.bitmap);
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
            if (entity.loadPolicy != HoverLoadPolicy.ONLY_NET) {
                //保存到缓存中
                HoverCacheManger.getInstance().addBitmapToCache(key, entity.bitmap);
            }
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
            width = getImageViewFieldValue(imageView, "mMaxWidth");
        }
        if (width <= 0) {
            width = displayMetrics.widthPixels;
        }
        // Get actual bitmap height
        int height = params.height == ViewGroup.LayoutParams.WRAP_CONTENT ? 0 : imageView.getHeight();
        if (height <= 0) {
            height = getImageViewFieldValue(imageView, "mMaxHeight");
        }
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
