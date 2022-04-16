package com.g10.util;

import com.matei.eece411.util.ByteOrder;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;

public class ByteUtil {
    /**
     * Convert a long integer value to a sequence of bytes in little-endian.
     *
     * @param src    the value to convert.
     * @param buffer buffer to store the converted value.
     * @param offset where in the buffer to store the converted value.
     */
    public static void longToBytes(long src, byte[] buffer, int offset) {
        for (int i = offset; i < offset + Byte.SIZE; i++) {
            buffer[i] = (byte) src;
            src >>= Byte.SIZE;
        }
    }

    public static byte[] int2leb(int x) {
        byte[] result = new byte[4];
        ByteOrder.int2leb(x, result, 0);
        return result;
    }

    /**
     * Concatenate sequences of bytes in the given order.
     *
     * @param arrays the sequences of bytes to concatenate.
     * @return a sequence of bytes.
     */
    public static byte[] concat(byte[]... arrays) {
        /* Total length of the arrays */
        int length = Arrays.stream(arrays)
                .mapToInt(value -> value.length)
                .reduce(0, Integer::sum);

        ByteBuffer resultBuffer = ByteBuffer.allocate(length);
        Arrays.stream(arrays).forEach(resultBuffer::put);
        return resultBuffer.array();
    }

    /**
     * Calculate the CRC32 check sum of a sequence of bytes
     *
     * @param data data of which to calculate the checksum
     * @return checksum
     */
    public static long getCheckSum(byte[] data) {
        CRC32 crc32 = new CRC32();
        crc32.update(data, 0, data.length);
        return crc32.getValue();
    }

    /**
     * Calculate the CRC32 check sum of bytes in a ByteBuffer
     *
     * @param bytes bytes of which to calculate the checksum
     * @return checksum
     */
    public static long getCheckSum(ByteBuffer bytes) {
        CRC32 crc32 = new CRC32();
        crc32.update(bytes);
        return crc32.getValue();
    }

    /**
     * Adapted from StringUtils.byteArrayToHexString(byte[] bytes)
     */
    public static String bytesToHexString(Iterable<Byte> bytes) {
        StringBuilder buffer = new StringBuilder();

        for (Byte b : bytes) {
            int val = ByteOrder.ubyte2int(b);
            String str = Integer.toHexString(val);
            for (int i = str.length(); i < 2; i++) {
                buffer.append('0');
            }
            buffer.append(str);
        }

        return buffer.toString().toUpperCase();
    }

    public static byte[] copyToByteArray(ByteBuffer bb) {
        byte[] result = new byte[bb.remaining()];
        bb.get(result);
        return result;
    }
}
