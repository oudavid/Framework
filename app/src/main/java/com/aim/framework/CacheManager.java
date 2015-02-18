package com.aim.framework;

import android.content.Context;

/**
 * Created by Administrator on 2/18/15.
 */
public class CacheManager {
    private static final String CACHE_PREF_KEY = "cache";

    private static Cache sCache;

    /**
     * Safely get the cache mechanism
     * @param context
     * @return
     */
    public static Cache getCache(Context context) {
        if(sCache == null) {
            synchronized (CacheManager.class) {
                if(sCache == null) {
                    sCache = new SharedPreferenceCache(context, CACHE_PREF_KEY);
                }
            }
        }
        return sCache;
    }
}
