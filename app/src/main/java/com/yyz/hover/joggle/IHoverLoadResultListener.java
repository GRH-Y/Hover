package com.yyz.hover.joggle;

import android.graphics.Bitmap;
import android.widget.ImageView;

/**
 * 图片加载监听器
 */
public interface IHoverLoadResultListener {
    /**
     * 加载回调
     *
     * @param url
     * @param view
     * @param imageData
     * @param image
     */
    void loadResultCallBack(String url, ImageView view, byte[] imageData, Bitmap image);

//    /**
//     * 加载图片失败回调
//     *
//     * @param view
//     */
//        void errorCallBack(ImageView view);
}
