package com.g10.cpen431.a8;

import com.g10.util.Tuple;

import java.nio.ByteBuffer;

public interface KeyValueStorage {
    boolean put(byte[] key, ByteBuffer value, int version);

    Tuple<byte[], Integer> get(byte[] key);

    boolean remove(byte[] key);

    void wipeOut();
}
