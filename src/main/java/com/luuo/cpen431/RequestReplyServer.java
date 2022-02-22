package com.luuo.cpen431;

import ca.NetSysLab.ProtocolBuffers.Message;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.luuo.util.ByteUtil;
import com.luuo.util.NotificationCenter;
import com.luuo.util.SystemUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.time.Duration;

public class RequestReplyServer implements Closeable {
    private static final Logger logger = LogManager.getLogger(RequestReplyServer.class);

    private static final int ID_SIZE = 16; /* 16B */
    private static final int MAX_PAYLOAD_SIZE = 16 * 1024; /* 16KB */
    private static final int MAX_PACKET_SIZE = 17 * 1024; /* 16KB */
    private static final Duration TIMEOUT = Duration.ofSeconds(6);

    private final RequestReplyApplication application;
    private final Cache<ByteString, byte[]> cache;
    private final DatagramSocket socket;

    public RequestReplyServer(int port, RequestReplyApplication application) throws IOException {
        this.socket = new DatagramSocket(port);
        this.application = application;
        this.cache = CacheBuilder.newBuilder()
                .expireAfterWrite(TIMEOUT)
                .concurrencyLevel(SystemUtil.concurrencyLevel())
                .build();
        /* don't use max size of weight */ // TODO: explain

        NotificationCenter.subscribeMemoryStress(this.cache::cleanUp);

        logger.info("RequestReplyServer initialized. Socket bound to port {}", port);
    }

    public void run() throws IOException {
        byte[] receiveBuffer = new byte[MAX_PACKET_SIZE];
        DatagramPacket packet = new DatagramPacket(receiveBuffer, MAX_PACKET_SIZE);
        while (!socket.isClosed()) {
            socket.receive(packet);
            processPacket(packet);
            packet.setData(receiveBuffer);
        }
        logger.info("Socket is closed.");
    }

    public void close() {
        logger.info("Closing socket.");
        socket.close();
    }

    private void processPacket(DatagramPacket packet) throws IOException {
        logger.info("Processing packet: {}", packet);
        Message.Msg message;
        try {
            message = Message.Msg.PARSER.parseFrom(packet.getData(), 0, packet.getLength());
        } catch (InvalidProtocolBufferException e) {
            logger.warn("Unable to parse packet. Packet: {}", packet);
            return;
        }

        ByteString id = message.getMessageID();
        ByteString requestPayload = message.getPayload();
        long checksum = message.getCheckSum();

        logger.info("Packet has id: {}", () -> ByteUtil.bytesToHexString(id));

        /* Check id and requestPayload sizes */
        if (id.size() != ID_SIZE) {
            logger.warn("ID length is {}, but expected {}", id.size(), ID_SIZE);
            return;
        }
        if (requestPayload.size() > MAX_PAYLOAD_SIZE) {
            logger.warn("Payload is too large. Size = {}", requestPayload.size());
            return;
        }

        /* Check checksum */
        long expectedChecksum = getCheckSum(id, requestPayload);
        if (checksum != expectedChecksum) {
            logger.warn("Mismatch checksums. Packet: {}", packet);
            return;
        }

        byte[] reply = cache.getIfPresent(id);
        if (reply == null) {
            RequestReplyApplication.Reply respondPayload = application.handleRequest(requestPayload.newInput());
            reply = constructResponse(id, respondPayload.reply);
            if (!respondPayload.idempotent) {
                cache.put(id, reply);
            }
        }

        packet.setData(reply);
        socket.send(packet);
        logger.info("Response packet sent. {}", packet);
    }

    private byte[] constructResponse(ByteString id, ByteString payload) {
        long responseChecksum = getCheckSum(id, payload);
        return Message.Msg.newBuilder()
                .setMessageID(id)
                .setPayload(payload)
                .setCheckSum(responseChecksum)
                .build().toByteArray();
    }

    private long getCheckSum(ByteString id, ByteString payload) {
        return ByteUtil.getCheckSum(id.concat(payload).asReadOnlyByteBuffer());
    }
}
