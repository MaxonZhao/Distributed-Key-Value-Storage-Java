package com.g10.cpen431.a11.membership;

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
        ByteBuffer byte_buf = ByteBuffer.wrap(key);

        ByteOrder byteOrder = byte_buf.order();
        byte_buf.order(ByteOrder.LITTLE_ENDIAN);

        long h = SEED ^ (byte_buf.remaining() * parameter);
        long k;
        while (byte_buf.remaining() >= 8) {
            k = byte_buf.getLong();
            k *= parameter;
            k ^= k >>> position;
            k *= parameter;
            h ^= k;
            h *= parameter;
        }
        if (byte_buf.remaining() > 0) {
            ByteBuffer finish = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
            finish.put(byte_buf).rewind();
            h ^= finish.getLong();
            h *= parameter;
        }
        h ^= h >>> position;
        h *= parameter;
        h ^= h >>> position;

        byte_buf.order(byteOrder);
        return h;
    }

}
