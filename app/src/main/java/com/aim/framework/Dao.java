package com.aim.framework;

/**
 * Created by Administrator on 2/18/15.
 */
public interface Dao {
    /**
     * Initialize this DAO. This should load any cached data if there was any saved.
     */
    void initializeFromCache() throws IllegalAccessException;

    /**
     * Clear the DAO's of user data, this can happen from logging out
     */
    void onSignOutSession();

    /**
     * Has this DAO been loaded? Sometimes its hard to tell due to no data somtimes equals ok.
     * Up to each implementation.
     * @return
     */
    boolean hasBeenInited();

    /**
     * Method which will be called when an outsider would like to prompt the DAO to save its
     * cache. NOTE: This should really only be called by the DAO itself, however there is one
     * case that we let the UserManager call saveToCache to immediately push data to cache when it
     * needs to be encrypted.
     */
    void saveToCache();
}
