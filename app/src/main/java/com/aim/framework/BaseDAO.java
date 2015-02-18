package com.aim.framework;

import android.content.Context;

/**
 * Created by Administrator on 2/18/15.
 */
public abstract class BaseDAO implements DAO {
    private static final String TAG = BaseDAO.class.getSimpleName();

    protected final Context mContext;
    protected String mCacheKey;
    protected CacheComposite mCacheComposite;

    public BaseDAO(Context context) {
        mContext = context;
    }

    @Override
    public void initializeFromCache() throws IllegalAccessException {
        if (StringUtils.isNullOrEmpty(mCacheKey)) {
            throw new IllegalAccessException("Cache key is null/empty!");
        }
        mCacheComposite = CacheManager.getCache(mContext).get(mCacheKey);
    }

    @Override
    public void onSignOutSession() {
        CacheManager.getCache(mContext).remove(mCacheKey);
    }

    @Override
    public boolean hasBeenInited() {
        return mCacheComposite != null;
    }

    @Override
    public void saveToCache() {
        CacheManager.getCache(mContext).put(mCacheKey, mCacheComposite);
    }
}
