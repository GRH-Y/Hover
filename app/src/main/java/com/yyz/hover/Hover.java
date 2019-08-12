package com.yyz.hover;


import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.DrawableRes;
import android.widget.ImageView;

import com.yyz.hover.entity.HoverLoadImageEntity;
import com.yyz.hover.joggle.IHoverLoadHandleListener;
import com.yyz.hover.joggle.IHoverLoadResultListener;

import java.io.File;

import log.LogDog;
import util.StringEnvoy;


/**
 * 简易加载图片工具类
 * Created by prolog on 5/11/2017.
 *
 * @author yyz
 */
public class Hover {

    private volatile static Hover util = null;

    public static Hover getInstance() {
        if (util == null) {
            synchronized (Hover.class) {
                if (util == null) {
                    util = new Hover();
                }
            }
        }
        return util;
    }

    private Hover() {
    }

    public void init(Context context, int thread) {
        HoverUIRefreshHandler.getInstance();
        HoverCacheManger.getInstance().init(context);
        HoverTaskAllocationManger taskAllocationManger = HoverTaskAllocationManger.getInstance();
        for (int count = 0; count < thread; count++) {
            taskAllocationManger.addExecutor(new HoverImageLoadTask());
        }
        taskAllocationManger.startLoad();
    }

    public void init(Context context) {
        init(context, 2);
    }

    public void setTimeout(int timeout) {
        HoverTaskAllocationManger.getInstance().setTimeout(timeout);
    }

    public void pauseDispose() {
        HoverTaskAllocationManger.getInstance().pauseDispose();
        HoverTaskAllocationManger.getInstance().clearSubmitTask();
    }

    public void continueDispose() {
        HoverTaskAllocationManger.getInstance().continueDispose();
    }

    public void destroy() {
        HoverTaskAllocationManger.getInstance().destroy();
        HoverTaskAllocationManger.getInstance().clearSubmitTask();
        util = null;
    }

    public void addImageToCache(String path, Bitmap bitmap) {
        if (StringEnvoy.isNotEmpty(path) && bitmap != null && !bitmap.isRecycled()) {
            String key = HoverCacheManger.getInstance().getUrlBase64(path.getBytes());
            HoverCacheManger.getInstance().addBitmapToCache(key, bitmap);
        }
    }

    public void getImageForCache(String path) {
        if (StringEnvoy.isNotEmpty(path)) {
            String key = HoverCacheManger.getInstance().getUrlBase64(path.getBytes());
            HoverCacheManger.getInstance().getBitmapFromCache(key);
        }
    }

    public void removerImageForCache(String path) {
        if (StringEnvoy.isNotEmpty(path)) {
            String key = HoverCacheManger.getInstance().getUrlBase64(path.getBytes());
            HoverCacheManger.getInstance().removerForKey(key);
        }
    }

    public void clearCacheImage() {
        HoverCacheManger.getInstance().clearCacheImage();
    }

    public void clearDiskImage() {
        HoverCacheManger.getInstance().clearDiskImage();
    }

    /**
     * 判断该地址图片是否以及加载在本地(检测内存缓存和磁盘缓存)
     *
     * @param path
     * @return
     */
    public boolean isHasLoad(String path) {
        return HoverCacheManger.getInstance().isHasLoad(path);
    }

    public File isHasDisk(String path) {
        return HoverCacheManger.getInstance().isHasDisk(path);
    }

    public Bitmap getCacheBitmap(String path) {
        return HoverCacheManger.getInstance().getBitmapFromPath(path);
    }

    public boolean isBigBitmap(String path) {
        return HoverBitmapHelper.isBigBitmap(path);
    }

    public boolean isBigBitmap(byte[] bitmapData) {
        return HoverBitmapHelper.isBigBitmap(bitmapData);
    }

    // -----------------------------loadImage ------------------------------------------------------

    public void loadImage(byte[] data, ImageView view) {
        loadImage(data, 0, view);
    }

    public void loadImage(byte[] data, @DrawableRes int errorImageRid, ImageView view) {
        loadImage(data, errorImageRid, view, null);
    }

    public void loadImage(byte[] data, @DrawableRes int errorImageRid, ImageView view, IHoverLoadResultListener listener) {
        loadImage(data, errorImageRid, view, null, listener);
    }

    public void loadImage(byte[] data, @DrawableRes int errorImageRid, ImageView view, IHoverLoadHandleListener handleListener,
                          IHoverLoadResultListener resultListener) {
        loadImage(data, null, errorImageRid, view, null, handleListener, resultListener);
    }

    public byte[] downloadSyncImage(String path) {
        return HoverTaskAllocationManger.getInstance().downloadSyncImage(path);
    }

    public void downloadImage(String path) {
        loadImage(path, 0, null, null, null, null);
    }

    public void downloadImage(String path, IHoverLoadResultListener listener) {
        loadImage(path, 0, null, null, null, listener);
    }

    public void downloadImage(String path, IHoverLoadResultListener listener, HoverLoadPolicy networkPolicy) {
        loadImage(path, 0, null, networkPolicy, null, listener);
    }

    public void loadImage(String path, ImageView view) {
        loadImage(path, 0, view, null, null, null);
    }

    public void loadImage(String path, ImageView view, HoverLoadPolicy networkPolicy) {
        loadImage(path, 0, view, networkPolicy, null, null);
    }

    public void loadImage(String path, ImageView view, IHoverLoadResultListener listener) {
        loadImage(path, 0, view, null, null, listener);
    }

    public void loadImage(String path, ImageView view, IHoverLoadHandleListener handleListener, IHoverLoadResultListener resultListener) {
        loadImage(path, 0, view, null, handleListener, resultListener);
    }

    public void loadImage(String path, @DrawableRes int errorImageRid, ImageView view) {
        loadImage(path, errorImageRid, view, null, null, null);
    }

    public void loadImage(String path, @DrawableRes int errorImageRid, ImageView view, IHoverLoadResultListener listener) {
        loadImage(path, errorImageRid, view, null, null, listener);
    }

    public void loadImage(String path, @DrawableRes int errorImageRid, ImageView view,
                          IHoverLoadHandleListener handleListener, IHoverLoadResultListener resultListener) {
        loadImage(path, errorImageRid, view, null, handleListener, resultListener);
    }

    public void loadImage(String path, @DrawableRes int errorImageRid, ImageView view,
                          HoverLoadPolicy networkPolicy, IHoverLoadResultListener resultListener) {
        loadImage(path, errorImageRid, view, networkPolicy, null, resultListener);
    }

    public void loadImage(String path, @DrawableRes int errorImageRid, ImageView view,
                          HoverLoadPolicy networkPolicy, IHoverLoadHandleListener handleListener,
                          IHoverLoadResultListener resultListener) {

        loadImage(null, path, errorImageRid, view, networkPolicy, handleListener, resultListener);
    }

    private void loadImage(byte[] data, String path, @DrawableRes int errorImageRid, ImageView view,
                           HoverLoadPolicy networkPolicy, IHoverLoadHandleListener handleListener,
                           IHoverLoadResultListener resultListener) {

        if ((data == null && path == null) || (data != null && view == null)) {
            LogDog.e("==> loadImage() data or path Can't be empty ! ");
            return;
        }

        if (!path.startsWith(HoverImageLoadTask.TAG_HTTP) && !path.startsWith(HoverImageLoadTask.TAG_FILE)) {
            LogDog.e("==> loadImage path illegal address ! " + path);
            return;
        }

        HoverLoadImageEntity entity = new HoverLoadImageEntity();
        if (path == null) {
            entity.imageData = data;
        } else {
            entity.path = path;
        }
        entity.view = view;
        entity.loadPolicy = networkPolicy;
        entity.errorImageRid = errorImageRid;
        entity.resultListener = resultListener;
        entity.handleListener = handleListener;

        HoverTaskAllocationManger.getInstance().submitTask(entity);

    }

}
