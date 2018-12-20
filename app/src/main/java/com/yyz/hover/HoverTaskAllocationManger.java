package com.yyz.hover;


import com.yyz.hover.entity.HoverLoadImageEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 任务分配器
 *
 * @className: HoverTaskAllocationManger
 * @classDescription:
 * @author: yyz
 * @createTime: 9/10/2018
 */
public class HoverTaskAllocationManger {

    private static volatile HoverTaskAllocationManger sTaskManger;
    private Map<String, HoverLoadImageEntity> mLoadEntityCache;
    private List<HoverImageLoadTask> mExecutorList;
    private int currentIndex = 0;
    private Lock mLock;

    private HoverTaskAllocationManger() {
        mLock = new ReentrantLock();
        mExecutorList = new ArrayList<>();
        mLoadEntityCache = new HashMap<>();
    }

    protected static HoverTaskAllocationManger getInstance() {
        if (sTaskManger == null) {
            synchronized (HoverTaskAllocationManger.class) {
                if (sTaskManger == null) {
                    sTaskManger = new HoverTaskAllocationManger();
                }
            }
        }
        return sTaskManger;
    }

    protected byte[] downloadSyncImage(String path) {
        if (mExecutorList.size() > 0) {
            return mExecutorList.get(0).downloadSyncImage(path);
        }
        return null;
    }


    protected void addExecutor(HoverImageLoadTask executor) {
        mLock.lock();
        try {
            mExecutorList.add(executor);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mLock.unlock();
        }
    }

    protected void startLoad() {
        mLock.lock();
        try {
            for (HoverImageLoadTask loadTask : mExecutorList) {
                loadTask.getExecutor().startTask();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mLock.unlock();
        }
    }

    protected void setTimeout(int timeout) {
        mLock.lock();
        try {
            for (HoverImageLoadTask loadTask : mExecutorList) {
                loadTask.setTimeout(timeout);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mLock.unlock();
        }
    }

    protected void pauseDispose() {
        mLock.lock();
        try {
            for (HoverImageLoadTask loadTask : mExecutorList) {
                loadTask.getExecutor().pauseTask();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mLock.unlock();
        }
    }

    protected void continueDispose() {
        mLock.lock();
        try {
            for (HoverImageLoadTask loadTask : mExecutorList) {
                loadTask.getExecutor().resumeTask();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mLock.unlock();
        }
    }

    protected void destroy() {
        mLock.lock();
        try {
            for (HoverImageLoadTask loadTask : mExecutorList) {
                loadTask.getExecutor().stopTask();
            }
            mExecutorList.clear();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mLock.unlock();
        }
    }


    protected void submitTask(HoverLoadImageEntity entity) {
        mLock.lock();
        if (entity.view != null) {
            mLoadEntityCache.put(entity.view.toString(), entity);
        } else {
            mLoadEntityCache.put(entity.path, entity);
        }
        try {
            HoverImageLoadTask loadTask = mExecutorList.get(currentIndex);
            loadTask.getExecutor().resumeTask();
            if (currentIndex == mExecutorList.size() - 1) {
                currentIndex = 0;
            } else {
                currentIndex++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mLock.unlock();
        }
    }

    protected HoverLoadImageEntity getNewTask() {
        HoverLoadImageEntity entity = null;
        mLock.lock();
        try {
            Object[] array = mLoadEntityCache.keySet().toArray();
            if (array.length > 0) {
                String key = array[0].toString();
                entity = mLoadEntityCache.remove(key);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mLock.unlock();
        }
        return entity;
    }

    protected void clearSubmitTask() {
        mLock.lock();
        try {
            mLoadEntityCache.clear();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mLock.unlock();
        }
    }
}
