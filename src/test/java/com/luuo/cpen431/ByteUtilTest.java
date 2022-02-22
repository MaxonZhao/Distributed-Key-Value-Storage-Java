package com.luuo.cpen431;

import com.luuo.util.ByteUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ByteUtilTest {
    @Test
    void longToBytesTest() {
        byte[] buffer = new byte[8];
        ByteUtil.longToBytes(0x12FAF6AB78L, buffer, 0);

        byte[] expected = new byte[]{
                (byte) 0x78, (byte) 0xAB, (byte) 0xF6, (byte) 0xFA,
                (byte) 0x12, (byte) 0x00, (byte) 0x00, (byte) 0x00};
        assertArrayEquals(expected, buffer);
    }

    @Test
    void concatTest() {
        byte[] first = {1, 2, 3, 4};
        byte[] second = {5, 6, 7, 8};

        byte[] expected = {1, 2, 3, 4, 5, 6, 7, 8};

        assertArrayEquals(expected, ByteUtil.concat(first, second));
    }

    @Test
    void getCheckSumTest() {
        byte[] bytes = {1, 2, 3, 4, 5, 6, 7, 8};

        long checksum = ByteUtil.getCheckSum(bytes);

        assertEquals(1070237893, checksum);
    }
}
