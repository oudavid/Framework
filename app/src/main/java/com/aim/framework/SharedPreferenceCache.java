package com.aim.framework;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.google.api.client.json.JsonGenerator;
import com.google.api.client.json.JsonParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.gson.Gson;

import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.HashMap;

/**
 * Caching mechanism built on top of shared preferences.
 *
 * Created by gcole on 8/11/14.
 */
public class SharedPreferenceCache implements Cache {

    private static final String TAG = SharedPreferenceCache.class.getName();

    private final Context mContext;
    private final String mSharedPrefKey;
    private final Jsonizer mJsonizer;
    private final HashMap<String, Object> mNonSerializedObjectCache;

    public SharedPreferenceCache(Context context, String sharedPrefKey) {
        mContext = context.getApplicationContext();
        mSharedPrefKey = sharedPrefKey;
        mJsonizer = new Jacksonizer();
        mNonSerializedObjectCache = new HashMap<>();
    }

    @Override
    public CacheComposite put(String key, CacheComposite item) {
        mNonSerializedObjectCache.put(key, item);
        String serializedData;

        try {
            serializedData = safeSerialize(item);

        } catch(JsonizeException e) {
            Log.e(TAG, "Unable to save to shared prefs: [" + key + ", " + item + "]", e);
            return item;
        }

        if (serializedData != null) {
            saveToPrefs(key, serializedData);
        }

        return item;
    }

    @Override
    public CacheComposite get(String key) {
        if(mNonSerializedObjectCache.containsKey(key)) {
            return (CacheComposite) mNonSerializedObjectCache.get(key);
        }

        // otherwise fetch it from the SharedPrefs
        String serializedData = readFromPrefs(key);
        try {
            CacheComposite data = safeDeserialize(serializedData, CacheComposite.class);

            // store to memory cache
            mNonSerializedObjectCache.put(key, data);
            return data;

        } catch(JsonizeException e) {
            Log.e(TAG, "Unable to get from shared prefs: [" + key + ", " + CacheComposite.class + "]", e);
            return null;
        }
    }

    @Override
    public void remove(String key) {
        mNonSerializedObjectCache.remove(key);
        mContext.getSharedPreferences(mSharedPrefKey, Context.MODE_PRIVATE)
                .edit()
                .remove(key)
                .commit();
    }

    @Override
    public void clearAll() {
        mContext.getSharedPreferences(mSharedPrefKey, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
    }

    /**
     * Save this json blob to prefs, can be null
     * @param key
     * @param data
     */
    private void saveToPrefs(String key, String data) {
        mContext.getSharedPreferences(mSharedPrefKey, Context.MODE_PRIVATE)
                .edit()
                .putString(key, data)
                .commit();
    }

    /**
     * retrieve this json blob from prefs, null if doesnt exist
     *
     * @param key
     * @return
     */
    private String readFromPrefs(String key) {
        return mContext.getSharedPreferences(mSharedPrefKey, Context.MODE_PRIVATE).getString(key, null);
    }

    /**
     * Safely deserialize the data to avoid NPEs
     *
     * @param raw
     * @param itemClazz
     * @param <T>
     * @return
     */
    private <T> T safeDeserialize(String raw, Class<T> itemClazz) throws JsonizeException {
        if(TextUtils.isEmpty(raw)) {
            return null;
        }

        return mJsonizer.deserialize(raw, itemClazz);
    }

    /**
     * Safely deserialize the data to avoid NPEs
     *
     * @param raw
     * @param type
     * @param <T>
     * @return
     */
    private <T> T safeDeserialize(String raw, Type type) throws JsonizeException {
        if(TextUtils.isEmpty(raw)) {
            return null;
        }

        return mJsonizer.deserialize(raw, type);
    }

    /**
     * Safely serialize data to avoid NPEs
     * @param o
     * @return
     */
    private String safeSerialize(Object o) throws JsonizeException {
        if(o == null) {
            return null;
        }


        return mJsonizer.serialize(o);
    }

    /**
     * Maybe we want to use a different serializer later
     */
    private interface Jsonizer {
        public String serialize(Object o) throws JsonizeException;
        public <E> E deserialize(String raw, Class<E> itemClazz) throws JsonizeException;
        public <E> E deserialize(String raw, Type type) throws JsonizeException;
    }


    /**
     * Simple exception wrapper to handle
     */
    static class JsonizeException extends Exception {
        private JsonizeException(Throwable throwable) {
            super(throwable);
        }
    }


    /**
     * Jackson as primary json parser
     */
    private static class Jacksonizer implements Jsonizer {
        private JacksonFactory mJackson = new JacksonFactory();

        @Override
        public String serialize(Object o) throws JsonizeException {
            StringWriter stringWriter = new StringWriter();
            try {
                JsonGenerator generator = mJackson.createJsonGenerator(stringWriter);
                generator.serialize(o);
                generator.close();
                return stringWriter.toString();
            }
            catch(Exception e) {
                throw new JsonizeException(e);
            }
        }

        @Override
        public <E> E deserialize(String raw, Class<E> itemClazz) throws JsonizeException {
            try {
                JsonParser parser = mJackson.createJsonParser(raw);
                return parser.parseAndClose(itemClazz);
            }
            catch(Exception e) {
                throw new JsonizeException(e);
            }
        }

        @Override
        public <E> E deserialize(String raw, Type type) throws JsonizeException {
            try {
                JsonParser parser = mJackson.createJsonParser(raw);
                return (E)parser.parse(type, true);
            }
            catch(Exception e) {
                throw new JsonizeException(e);
            }
        }
    }

    /**
     * Simple GSON serializer
     */
    private static class Gsonizer implements Jsonizer {
        private Gson mGson = new Gson();

        @Override
        public String serialize(Object o) throws JsonizeException {
            try {
                return mGson.toJson(o);
            }
            catch(Exception e) {
                throw new JsonizeException(e);
            }
        }

        @Override
        public <E> E deserialize(String raw, Class<E> itemClazz) throws JsonizeException {
            try {
                return mGson.fromJson(raw, itemClazz);
            }
            catch(Exception e) {
                throw new JsonizeException(e);
            }
        }

        @Override
        public <E> E deserialize(String raw, Type type) throws JsonizeException {
            try {
                return mGson.fromJson(raw, type);
            }
            catch(Exception e) {
                throw new JsonizeException(e);
            }
        }
    }
}