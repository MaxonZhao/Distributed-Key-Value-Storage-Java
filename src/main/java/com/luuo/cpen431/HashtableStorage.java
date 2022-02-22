package com.luuo.cpen431;

import com.luuo.util.ByteUtil;
import com.luuo.util.Tuple;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class HashtableStorage implements KeyValueStorage {
    private final ConcurrentHashMap<List<Byte>, Tuple<byte[], Integer>> map;

    public HashtableStorage() {
        this.map = new ConcurrentHashMap<>();
    }

    @Override
    public boolean put(ByteBuffer key, ByteBuffer value, int version) {
        List<Byte> mapKey = ByteUtil.copyToList(key);
        Tuple<byte[], Integer> mapValue = new Tuple<>(ByteUtil.copyToByteArray(value), version);
        map.put(mapKey, mapValue);
        return true;
    }

    @Override
    public Tuple<byte[], Integer> get(ByteBuffer key) {
        List<Byte> mapKey = ByteUtil.copyToList(key);
        return map.get(mapKey);
    }

    @Override
    public boolean remove(ByteBuffer key) {
        List<Byte> mapKey = ByteUtil.copyToList(key);
        return map.remove(mapKey) != null;
    }

    @Override
    public void wipeOut() {
        map.clear();
    }
}
