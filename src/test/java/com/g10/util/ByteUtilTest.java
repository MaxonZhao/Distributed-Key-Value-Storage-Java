package com.g10.util;

import com.g10.cpen431.MembershipMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ByteUtilTest {
    @Test
    void testLongToBytes() {
        byte[] buffer = new byte[8];
        ByteUtil.longToBytes(0x12FAF6AB78L, buffer, 0);

        byte[] expected = new byte[]{
                (byte) 0x78, (byte) 0xAB, (byte) 0xF6, (byte) 0xFA,
                (byte) 0x12, (byte) 0x00, (byte) 0x00, (byte) 0x00};
        assertArrayEquals(expected, buffer);
    }

    @Test
    void testConcat() {
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
    void testMembershipMessage() throws InvalidProtocolBufferException {
        long value1 = 276763243;
        long value2 = 123342431;
        byte[] ans = MembershipMessage.newBuilder()
                .addTimeVector(value1)
                .addTimeVector(value2)
                .build().toByteArray();

        long actualValue1 = MembershipMessage.parser().parseFrom(ans, 0, ans.length).getTimeVector(0);
        long actualValue2 = MembershipMessage.parser().parseFrom(ans, 0, ans.length).getTimeVector(1);
        assertEquals(value1, actualValue1);
        assertEquals(value2, actualValue2);
    }
}
