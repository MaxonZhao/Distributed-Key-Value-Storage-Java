package com.g10.cpen431.a7;

import com.g10.util.ByteUtil;
import com.g10.util.NodeInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import com.g10.util.Hash;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

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

    @Test
    void checkNodeInfo() throws IOException {
        NodeInfo.ServerList a = NodeInfo.parseNodeInfo("src/main/java/com/g10/util/serverList.yml");
        System.out.println(a.toString());
    }
    void check_hash_with_same_key() {
        Hash for_test = new Hash();
        byte[] A = ("AAAAA").getBytes(StandardCharsets.UTF_8);
        System.out.println(for_test.hash(A));
        System.out.println(for_test.hash(A));
        boolean answer = for_test.hash(A).longValue()==for_test.hash(A).longValue();
        System.out.println(answer);
        assertEquals(true, answer);
    }
    @Test
    void check_hash_with_different_key() {
        Hash for_test = new Hash();
        byte[] A = "AAAAA".getBytes(StandardCharsets.UTF_8);
        byte[] B = "BBBBB".getBytes(StandardCharsets.UTF_8);


        assertEquals(false, for_test.hash(A).longValue()==for_test.hash(B).longValue());
    }

}
