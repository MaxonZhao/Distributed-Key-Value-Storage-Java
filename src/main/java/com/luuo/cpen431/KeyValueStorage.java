package com.luuo.cpen431;

import com.luuo.util.Tuple;

import java.nio.ByteBuffer;

public interface KeyValueStorage {
    boolean put(ByteBuffer key, ByteBuffer value, int version);

    Tuple<byte[], Integer> get(ByteBuffer key);

    boolean remove(ByteBuffer key);

    void wipeOut();
}
