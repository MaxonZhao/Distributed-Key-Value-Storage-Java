package com.g10.cpen431.a7;

import ca.NetSysLab.ProtocolBuffers.Message;
import com.g10.util.ByteUtil;
import com.g10.util.NodeInfo;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import com.g10.util.Hash;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import ca.NetSysLab.ProtocolBuffers.epidemic;

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
        NodeInfo.ServerList a = NodeInfo.parseNodeInfo("src/test/resources/testServerList.yml");
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

    @Test
    void check_proto(){
        long val1 = 276763243;
        long val2 = 123342431;
        long val3 = 867456353;
        byte[] ans= epidemic.Time_tag.newBuilder()
                .addTimeTag(val1)
                .addTimeTag(val2)
                .build().toByteArray();
        System.out.println(ans);

        try {
            long get_val1=epidemic.Time_tag.PARSER.parseFrom(ans, 0, ans.length).getTimeTag(0);
            System.out.println(get_val1);

            long get_val2=epidemic.Time_tag.PARSER.parseFrom(ans, 0, ans.length).getTimeTag(1);
            System.out.println(get_val2);

        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }

    @Test
    void check_seperate_equal() {
        Hash for_test = new Hash();

        final double longRange = ((double) Long.MAX_VALUE - Long.MIN_VALUE);
        long range = (long) (longRange/20);

        ArrayList<Long> answer = new ArrayList<>();
        answer = for_test.set_node_num(20);

        assertEquals(range, answer.get(1) - answer.get(0));
    }



}
