package com.g10.cpen431.a12;

import com.g10.util.ByteList;
import com.g10.util.ByteUtil;
import com.g10.util.TimerUtil;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A hash table storing all key-value pairs in the current node.
 */
@Log4j2
public class KeyValueStorage implements Iterable<Map.Entry<ByteList, KeyValueStorage.Value>> {
    private static final KeyValueStorage instance = new KeyValueStorage();
    private final ConcurrentHashMap<ByteList, Value> map;

    private KeyValueStorage() {
        this.map = new ConcurrentHashMap<>();

        if (log.isInfoEnabled()) {
            TimerUtil.scheduleAtFixedRate(5 * 1000, new TimerTask() {
                @Override
                public void run() {
                    log.info("Current number of entries in the map: {}", map.size());
                }
            });
        }
    }

    public static KeyValueStorage getInstance() {
        return instance;
    }

    /**
     * Insert a key-value pair to the storage. Replace the old value only if the
     * timestamp of the new value is greater than the timestamp of the old value.
     * Race condition may occur if the user attempts to insert multiple values
     * for the same key concurrently.
     *
     * @param key the key
     * @param value the value to be associated with the key
     * @param version version of the key-value pair
     * @param timestamp the timestamp associated with the value
     * @return true if the key-value pair is inserted, false otherwise
     */
    public boolean put(byte[] key, ByteBuffer value, int version, long timestamp) {
        if (timestamp == 0) {
            throw new IllegalArgumentException("timestamp is 0");
        }
        ByteList mapKey = new ByteList(key);
        Value oldValue = map.get(mapKey);
        /* Block values that are older than the current value */
        if (oldValue != null && oldValue.timestamp > timestamp) {
            return false;
        }
        Value newValue = new Value(ByteUtil.copyToByteArray(value), version, timestamp);
        map.put(mapKey, newValue);
        return true;
    }

    /**
     * Get the value associated with a key from the storage
     *
     * @param key the requested key
     * @return if the key exists, return the associated value. Otherwise, return null.
     */
    @Nullable
    public Value get(byte[] key) {
        ByteList mapKey = new ByteList(key);
        return map.get(mapKey);
    }

    /**
     * Remove a key from the storage
     *
     * @param key the key to remove
     * @return true if there was previously a mapping for key, false otherwise.
     */
    public boolean remove(byte[] key) {
        ByteList mapKey = new ByteList(key);
        return map.remove(mapKey) != null;
    }

    public void wipeOut() {
        map.clear();
    }

    public Iterator<Map.Entry<ByteList, Value>> iterator() {
        return map.entrySet().iterator();
    }

    @AllArgsConstructor
    public static class Value {
        public final byte[] value;
        public final int version;
        public final long timestamp;
    }
}
