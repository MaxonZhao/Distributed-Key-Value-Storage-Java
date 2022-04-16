package com.luuo2000.drpc;

import com.google.protobuf.ByteString;

import java.nio.ByteBuffer;
import java.util.Random;
import java.util.zip.CRC32;

public class DatagramRpcUtil {
    private static final Random RAND = new Random();

    public static byte[] buildRpcMessage(long id, long expiry, RpcMessage.MessageType messageType,
                                         int payloadType, ByteString payload) {
        RpcMessage.Builder builder = RpcMessage.newBuilder()
                .setId(id)
                .setExpiry(System.currentTimeMillis() + expiry)
                .setMessageType(messageType)
                .setPayloadType(payloadType)
                .setPayload(payload);
        long checksum = getChecksum(builder);
        return builder.setChecksum(checksum).build().toByteArray();
    }

    public static boolean validate(RpcMessage request) {
//        if (System.currentTimeMillis() > request.getExpiry()) {
//            return false;
//        }
        long requestChecksum = request.getChecksum();
        long actualChecksum = getChecksum(request);
        return requestChecksum == actualChecksum;
    }

    public static long getChecksum(RpcMessageOrBuilder request) {
        ByteBuffer buffer = ByteBuffer
                .allocate(8 + 8 + 4 + 4)
                .putLong(request.getId()).putLong(request.getExpiry())
                .putInt(request.getMessageTypeValue()).putInt(request.getPayloadType());
        CRC32 crc32 = new CRC32();
        crc32.update(buffer);
        crc32.update(request.getPayload().asReadOnlyByteBuffer());
        return crc32.getValue();
    }

    public static long generateId() {
        long upper = System.currentTimeMillis() << 32;
        long lower = RAND.nextInt() & 0xFFFFFFFFL;
        return upper | lower;
    }
}
