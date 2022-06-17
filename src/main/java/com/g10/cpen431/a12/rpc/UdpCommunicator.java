package com.g10.cpen431.a12.rpc;

import com.g10.cpen431.RpcMessage;
import com.g10.util.SystemUtil;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;

/**
 * Sending and Receiving messages via UDP
 */
@Log4j2
class UdpCommunicator {
    private static final int MAX_PACKET_SIZE = 32 * 1024; /* 32KB */

    private final DatagramSocket socket;

    UdpCommunicator(int port, int numberOfThreads) throws SocketException {
        this.socket = SystemUtil.createDatagramSocket(port);
        for (int i = 0; i < numberOfThreads; i++) {
            new Thread(this::listen, getClass().getName() + "-" + i).start();
        }
    }

    /**
     * Construct a UDP packet containing the message and send it to the target address.
     *
     * @param target the target address
     * @param message the message to send
     * @throws IOException if an I/O error occurs
     */
    void sendMessage(SocketAddress target, RpcMessage message) throws IOException {
        byte[] request = message.toByteArray();
        DatagramPacket packet = new DatagramPacket(request, request.length, target);
        socket.send(packet);
    }

    private void listen() {
        DatagramPacket packet = new DatagramPacket(new byte[MAX_PACKET_SIZE], MAX_PACKET_SIZE);
        while (!socket.isClosed()) {
            try {
                socket.receive(packet);
                RpcMessage message;
                try {
                    message = RpcMessage.parser().parseFrom(
                            packet.getData(), packet.getOffset(), packet.getLength()
                    );
                } catch (InvalidProtocolBufferException e) {
                    log.error(e);
                    continue;
                }
                RpcService.receiveMessage(message);
            } catch (IOException e) {
                log.warn(e);
            }
        }
    }
}
