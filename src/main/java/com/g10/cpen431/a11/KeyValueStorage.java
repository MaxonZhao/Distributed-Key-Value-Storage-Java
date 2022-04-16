package com.g10.cpen431.a11;

import com.g10.util.*;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

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

//        SystemUtil.subscribeSuspended(x -> wipeOut());
    }

    public static KeyValueStorage getInstance() {
        return instance;
    }

    public boolean put(byte[] key, ByteBuffer value, int version, long timestamp) {
        if (timestamp == 0) {
            throw new IllegalArgumentException("timestamp is 0");
        }
        ByteList mapKey = new ByteList(key);
        Value oldValue = map.get(mapKey);
        if (oldValue != null && oldValue.timestamp >= timestamp) {
            return false;
        }
        Value newValue = new Value(ByteUtil.copyToByteArray(value), version, timestamp);
        map.put(mapKey, newValue);
        return true;
    }

    public Value get(byte[] key) {
        ByteList mapKey = new ByteList(key);
        return map.get(mapKey);
    }

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
