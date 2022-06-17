package com.g10.cpen431.a12.coordinator;

import ca.NetSysLab.ProtocolBuffers.KeyValueResponse;
import ca.NetSysLab.ProtocolBuffers.RequestReplyMessage;
import com.g10.util.ByteUtil;
import com.g10.util.ClientAddress;
import com.g10.util.SystemUtil;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * The ClientCommunicator is responsible for accepting requests from clients and sending replies.
 */
@Log4j2
class ClientCommunicator {
    private static final int MAX_PACKET_SIZE = 17 * 1024; /* 17KB */
    private static final int ID_SIZE = 16; /* 16B */
    private static final int MAX_PAYLOAD_SIZE = 16 * 1024; /* 16KB */

    private final DatagramSocket socket;

    ClientCommunicator(int port, int numberOfThreads) throws SocketException {
        this.socket = SystemUtil.createDatagramSocket(port);

        for (int i = 0; i < numberOfThreads; i++) {
            new Thread(this::run, getClass().getName() + "-" + i).start();
        }
    }

    /**
     * Send a message to the client.
     *
     * @param message       data to send
     * @param clientAddress address of the client
     * @throws IOException if an error occurs
     */
    void replyToClient(byte[] message, ClientAddress clientAddress) throws IOException {
        socket.send(clientAddress.newPacket(message));
    }

    /**
     * Send a key-value response to the client
     *
     * @param messageId     ID of the message
     * @param clientAddress address of the client
     * @param response      response to send
     * @param idempotent    whether this response should be cached
     * @throws IOException if an error occurs
     */
    void replyToClient(ByteString messageId, ClientAddress clientAddress, KeyValueResponse response,
                       boolean idempotent) throws IOException {
        byte[] message = constructClientMessage(messageId, response.toByteString()).toByteArray();
        replyToClient(message, clientAddress);

        if (!idempotent) {
            CoordinatorHandlers.cache.put(messageId, message);
        }
    }

    private void run() {
        byte[] receiveBuffer = new byte[MAX_PACKET_SIZE];
        DatagramPacket packet = new DatagramPacket(receiveBuffer, MAX_PACKET_SIZE);
        while (!socket.isClosed()) {
            try {
                socket.receive(packet);
            } catch (IOException e) {
                log.warn(e);
                continue;
            }

            /* Parse */
            RequestReplyMessage message = parseClientPacket(packet.getData(), packet.getOffset(), packet.getLength());
            if (message == null) {
                continue;
            }

            ClientAddress clientAddress = new ClientAddress(packet.getAddress(), packet.getPort());

            Context context = new Context(message.getMessageID(), clientAddress, message.getPayload());
            CoordinatorHandlers.handle(context);
        }
    }

    private RequestReplyMessage constructClientMessage(ByteString id, ByteString payload) {
        long responseChecksum = getCheckSum(id, payload);
        return RequestReplyMessage.newBuilder()
                .setMessageID(id)
                .setPayload(payload)
                .setCheckSum(responseChecksum)
                .build();
    }

    private RequestReplyMessage parseClientPacket(byte[] packetData, int offset, int length) {
        RequestReplyMessage message;
        try {
            message = RequestReplyMessage.parser().parseFrom(packetData, offset, length);
        } catch (InvalidProtocolBufferException e) {
            log.warn("Unable to parse packet");
            return null;
        }

        ByteString id = message.getMessageID();
        ByteString requestPayload = message.getPayload();
        long checksum = message.getCheckSum();

        /* Check id and requestPayload sizes */
        if (id.size() != ID_SIZE) {
            log.warn("ID length is {}, but expected {}", id.size(), ID_SIZE);
            return null;
        }
        if (requestPayload.size() > MAX_PAYLOAD_SIZE) {
            log.warn("Payload is too large. Size = {}", requestPayload.size());
            return null;
        }

        /* Check checksum */
        long expectedChecksum = getCheckSum(id, requestPayload);
        if (checksum != expectedChecksum) {
            log.warn("Mismatch checksums. ");
            return null;
        }

        return message;
    }

    private long getCheckSum(ByteString id, ByteString payload) {
        return ByteUtil.getCheckSum(id.concat(payload).asReadOnlyByteBuffer());
    }
}
