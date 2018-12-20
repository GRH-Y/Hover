package com.yyz.hover;


import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.yyz.hover.entity.HoverLoadImageEntity;

import task.executor.ConsumerQueueAttribute;

public class HoverUIRefreshHandler {

    private static volatile HoverUIRefreshHandler sHandler;
    private ConsumerQueueAttribute mQueueAttribute;
    private HandlerTask mHandlerTask;

    private HoverUIRefreshHandler() {
        mHandlerTask = new HandlerTask();
        mQueueAttribute = new ConsumerQueueAttribute();
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

    public void pushData(HoverLoadImageEntity entity) {
        mQueueAttribute.pushToCache(entity);
        mHandlerTask.sendEmptyMessage(0);
    }

    private class HandlerTask extends Handler {
        private HandlerTask() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            HoverLoadImageEntity entity = (HoverLoadImageEntity) mQueueAttribute.popCacheData();
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

}
