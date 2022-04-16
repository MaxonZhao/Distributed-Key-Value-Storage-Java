package com.g10.cpen431.a12.membership;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class Hash {
    public static int SEED = 84525;
    public static int position = 47;
    public static long parameter = 0xc6a4a7935bd1e995L;

    /**
     * Based on https://www.cnblogs.com/hd-zg/p/5917758.html
     */
    public static long hash(byte[] key) {
        ByteBuffer buffer = ByteBuffer.wrap(key);

        ByteOrder originalOrder = buffer.order();
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        long h = SEED ^ (buffer.remaining() * parameter);
        long k;
        while (buffer.remaining() >= 8) {
            k = buffer.getLong();
            k *= parameter;
            k ^= k >>> position;
            k *= parameter;
            h ^= k;
            h *= parameter;
        }
        if (buffer.remaining() > 0) {
            ByteBuffer finish = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
            finish.put(buffer).rewind();
            h ^= finish.getLong();
            h *= parameter;
        }
        h ^= h >>> position;
        h *= parameter;
        h ^= h >>> position;

        buffer.order(originalOrder);
        return h;
    }
}
