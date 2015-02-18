package com.aim.framework;

/**
 * Expected methods for a caching mechanism.
 *
 * Created by gcole on 8/11/14.
 */
public interface Cache {
    /**
     * Put this value into the cache. Return the an object that may be the same evaluated item but different object in memory
     *
     * @param key
     * @param item
     * @return
     */
    CacheComposite put(String key, CacheComposite item);

    /**
     * Get this keyed item from the cache.
     *
     * @param key
     * @return
     */
    CacheComposite get(String key);

    /**
     * Remove the keyed item from cache.
     *
     * @param key
     * @return
     */
    public void remove(String key);

    /**
     * Wipe the cache clean.
     */
    void clearAll();
}
