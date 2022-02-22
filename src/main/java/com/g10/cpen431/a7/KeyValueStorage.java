package com.g10.cpen431.a7;

import com.g10.util.Tuple;

import java.nio.ByteBuffer;

public interface KeyValueStorage {
    boolean put(ByteBuffer key, ByteBuffer value, int version);

    Tuple<byte[], Integer> get(ByteBuffer key);

    boolean remove(ByteBuffer key);

    void wipeOut();
}
