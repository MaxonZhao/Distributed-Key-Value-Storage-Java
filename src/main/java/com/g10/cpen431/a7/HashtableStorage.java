package com.g10.cpen431.a7;

import com.g10.util.ByteUtil;
import com.g10.util.Tuple;
import com.google.common.primitives.Bytes;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class HashtableStorage implements KeyValueStorage {
    private final ConcurrentHashMap<List<Byte>, Tuple<byte[], Integer>> map;

    public HashtableStorage() {
        this.map = new ConcurrentHashMap<>();
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
