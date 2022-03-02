package com.g10.util;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class Hash {
    public static int SEED = 84525;
    public static int position = 47;
    public static long parameter = 0xc6a4a7935bd1e995L;

    private static int node_num = 0;


    public static ArrayList<Long> set_node_num(int node_num){
        node_num = node_num;

        final double longRange = ((double) Long.MAX_VALUE - Long.MIN_VALUE);
        long range = (long) (longRange/node_num);

        ArrayList<Long> node_hash_list = new ArrayList<>();

        for(int i = 0; i < node_num; i++){
            node_hash_list.add(Long.MIN_VALUE+(i*range));
        }

        return node_hash_list;
    }

    /**
     * Based on https://www.cnblogs.com/hd-zg/p/5917758.html
     *
     * @param key
     * @return
     */
    public static Long hash(byte[] key) {
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
        return Long.valueOf(h);
    }

}
