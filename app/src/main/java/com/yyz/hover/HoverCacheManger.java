package com.yyz.hover;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Base64;
import android.util.LruCache;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import log.LogDog;
import storage.FileHelper;
import util.StringEnvoy;

public class HoverCacheManger {

    private static volatile HoverCacheManger sCacheManger;
    private File mAppCacheDir = null;
    private static final String sCachePath = "/Hover_Image_Cache";
    /**
     * 图片缓存
     */
    private LruCache<String, Bitmap> mLruCache;

    private HoverCacheManger() {
    }

    public static HoverCacheManger getInstance() {
        if (sCacheManger == null) {
            synchronized (HoverCacheManger.class) {
                if (sCacheManger == null) {
                    sCacheManger = new HoverCacheManger();
                }
            }
        }
        return sCacheManger;
    }


    public void init(Context context) {
        // 获取应用程序最大可用内存
        final long maxMemory = Runtime.getRuntime().maxMemory();
        final int cacheSize = (int) (maxMemory / 6);
        mLruCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    return value.getAllocationByteCount();
                }
                return value.getRowBytes() * value.getHeight();
            }
        };

        if (context != null) {
            File dir = context.getExternalCacheDir();
            if (dir != null) {
                mAppCacheDir = new File(dir, sCachePath);
                boolean exists = mAppCacheDir.exists();
                if (!exists) {
                    exists = mAppCacheDir.mkdirs();
                }
                if (!exists) {
                    mAppCacheDir = null;
                    LogDog.e("Cache file creation failed !!!");
                }
            }
        }
    }

    /**
     * 从LruCache中获取一张图片，如果不存在就返回null。
     */
    protected Bitmap getBitmapFromCache(String key) {
        if (mLruCache == null) {
            return null;
        }
        return mLruCache.get(key);
    }

    /**
     * 往LruCache中添加一张图片
     *
     * @param key
     * @param bitmap
     */
    protected void addBitmapToCache(String key, Bitmap bitmap) {
        if (key == null || bitmap == null || mLruCache == null) {
            return;
        }
        mLruCache.put(key, bitmap);
    }

    protected void saveImageToFile(String key, byte[] bitmap) {
        if (key == null || bitmap == null || mAppCacheDir == null) {
            return;
        }
        File file = new File(mAppCacheDir, key);
        FileHelper.writeFileMemMap(file, bitmap, false);
    }

    protected void saveImageToFile(String key, Bitmap bitmap) {
        byte[] base64 = Base64.encode(key.getBytes(), Base64.URL_SAFE);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] processed = stream.toByteArray();
        File file = new File(mAppCacheDir, new String(base64));
        FileHelper.writeFileMemMap(file, processed, false);
        try {
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据key移除缓存
     *
     * @param key
     */
    public void removerForKey(String key) {
        if (mLruCache != null && StringEnvoy.isNotEmpty(key)) {
            mLruCache.remove(key);
        }
    }


    /**
     * 清除内存缓存的图片
     */
    public void clearCacheImage() {
        if (mLruCache != null) {
            mLruCache.evictAll();
        }
    }

    /**
     * 清除缓存在磁盘的图片
     */
    public void clearDiskImage() {
        if (mAppCacheDir != null && mAppCacheDir.exists()) {
            File[] files = mAppCacheDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
    }

    protected File isHasDisk(String path) {
        File isHas = null;
        String key = getUrlBase64(path.getBytes());
        if (key == null) {
            return null;
        }
        if (mAppCacheDir != null && mAppCacheDir.exists() && mAppCacheDir.isDirectory()) {
            isHas = new File(mAppCacheDir, key);
        }
        return isHas;
    }

    protected boolean isHasLoad(String path) {
        String key = getUrlBase64(path.getBytes());
        if (key == null) {
            return false;
        }
        Bitmap bitmap = getBitmapFromCache(key);
        boolean isHas = bitmap != null;
        if (bitmap == null) {
            if (mAppCacheDir != null && mAppCacheDir.exists() && mAppCacheDir.isDirectory()) {
                File file = new File(mAppCacheDir, key);
                isHas = file.exists();
            }
        }
        return isHas;
    }

    /**
     * 根据路径获取bitmap，先从内存缓存拿，没有则取磁盘获取
     * @param path 图片路径
     * @return
     */
    protected Bitmap getBitmapFromPath(String path) {
        String key = getUrlBase64(path.getBytes());
        Bitmap bitmap = getBitmapFromCache(key);
        if (bitmap == null) {
            if (mAppCacheDir != null && mAppCacheDir.exists() && mAppCacheDir.isDirectory()) {
                File file = new File(mAppCacheDir, key);
                if (file.exists()) {
                    bitmap = BitmapFactory.decodeFile(file.getPath());
                }
            }
        }
        return bitmap;
    }

    protected Bitmap getBitmapForFile(String key) {
        if (mAppCacheDir != null && mAppCacheDir.exists() && mAppCacheDir.isDirectory()) {
            File file = new File(mAppCacheDir, key);
            if (file.exists()) {
                return BitmapFactory.decodeFile(file.getPath());
            }
        }
        return null;
    }

    protected byte[] getBitmapForByte(String key) {
        byte[] data = null;
        String filePath = getBitmapFromFile(key);
        if (filePath != null) {
            data = FileHelper.readFileMemMap(filePath);
        }
        return data;
    }

    protected Bitmap getBitmapFromKey(String key) {
        Bitmap bitmap = getBitmapFromCache(key);
        if (bitmap == null) {
            bitmap = getBitmapForFile(key);
        }
        return bitmap;
    }

    /**
     * 根据key查找缓存中的文件
     *
     * @param key
     * @return
     */
    protected String getBitmapFromFile(String key) {
        if (mAppCacheDir != null && mAppCacheDir.exists() && mAppCacheDir.isDirectory()) {
            File file = new File(mAppCacheDir, key);
            if (file.exists()) {
                return file.getPath();
            }
        }
        return null;
    }


    public String getUrlBase64(byte[] data) {
        if (data == null) {
            return null;
        }
        byte[] base64 = Base64.encode(data, Base64.URL_SAFE);
        String strBase64 = new String(base64).replaceAll("=", "");
        return strBase64.replaceAll("\n", "");
    }
}
