package com.yyz.hover;

/**
 * 图片加载网络策略
 */
public enum HoverLoadPolicy {
    /**
     * ONLY_NET:    不管本地是否存在，都去网络请求获取最新的数据(不缓存)
     * ONLY_CACHE:  只从本地缓存中获取
     * CACHE_OR_NET: 如果本地缓存中存在则获取，不在请求网络获取
     * ALL : 如果本地有缓存则先去获取，同时异步去网络请求获取最新的数据
     */
    ONLY_NET, ONLY_CACHE, CACHE_OR_NET, ALL
}
