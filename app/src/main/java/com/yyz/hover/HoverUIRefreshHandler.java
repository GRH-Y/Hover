package com.yyz.hover;


import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.yyz.hover.entity.HoverLoadImageEntity;

public class HoverUIRefreshHandler extends Handler{

    private static volatile HoverUIRefreshHandler sHandler;

    private HoverUIRefreshHandler() {
        super(Looper.getMainLooper());
    }

    public static HoverUIRefreshHandler getInstance() {
        if (sHandler == null) {
            synchronized (Hover.class) {
                if (sHandler == null) {
                    sHandler = new HoverUIRefreshHandler();
                }
            }
        }
        return sHandler;
    }


    @Override
    public void handleMessage(Message msg) {
        HoverLoadImageEntity entity = (HoverLoadImageEntity) msg.obj;
        if (entity != null) {
            if (entity.bitmap == null && entity.errorImageRid > 0 && entity.view != null) {
                entity.view.setImageResource(entity.errorImageRid);
            } else if (entity.bitmap != null && entity.view != null) {
                entity.view.setImageBitmap(entity.bitmap);
            }
            if (entity.resultListener != null) {
                entity.resultListener.loadResultCallBack(entity.path, entity.view, entity.imageData, entity.bitmap);
            }
        }
    }

}
