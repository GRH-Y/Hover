package com.yyz.hover.entity;

import android.graphics.Bitmap;
import android.widget.ImageView;

import com.yyz.hover.HoverLoadPolicy;
import com.yyz.hover.joggle.IHoverLoadHandleListener;
import com.yyz.hover.joggle.IHoverLoadResultListener;


public class HoverLoadImageEntity {

    public IHoverLoadHandleListener handleListener;
    public IHoverLoadResultListener resultListener;
    public HoverLoadPolicy loadPolicy;
    public ImageView view = null;
    public byte[] imageData = null;
    public String path = null;
    public Bitmap bitmap = null;
    public int errorImageRid = 0;

    @Override
    public String toString() {
        return "HoverLoadImageEntity{" +
                ", view=" + view +
                ", path='" + path + '\'' +
                '}';
    }
}
