package com.g10.cpen431.a8;

import com.g10.util.ByteUtil;
import com.g10.util.Tuple;
import com.google.common.primitives.Bytes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class HashtableStorage implements KeyValueStorage {
    private static final Logger logger = LogManager.getLogger(HashtableStorage.class);
    private final ConcurrentHashMap<List<Byte>, Tuple<byte[], Integer>> map;

    public HashtableStorage() {
        this.map = new ConcurrentHashMap<>();

        if (logger.isInfoEnabled()) {
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    logger.info("Current number of entries in the map: {}", map.size());
                }
            }, 0, 5 * 1000);
        }
    }

    @Override
    public boolean put(byte[] key, ByteBuffer value, int version) {
        List<Byte> mapKey = Bytes.asList(key);
        Tuple<byte[], Integer> mapValue = new Tuple<>(ByteUtil.copyToByteArray(value), version);
        map.put(mapKey, mapValue);
        return true;
    }

    @Override
    public Tuple<byte[], Integer> get(byte[] key) {
        List<Byte> mapKey = Bytes.asList(key);
        return map.get(mapKey);
    }

    @Override
    public boolean remove(byte[] key) {
        List<Byte> mapKey = Bytes.asList(key);
        return map.remove(mapKey) != null;
    }

    @Override
    public void wipeOut() {
        map.clear();
    }
}
