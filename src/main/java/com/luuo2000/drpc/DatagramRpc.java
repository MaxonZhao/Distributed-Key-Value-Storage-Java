package com.luuo2000.drpc;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;

@Log4j2
public class DatagramRpc {
    private static final int UDP_BUFFER_SIZE = 64 * 1024 * 1024;
    private static final int MAX_PACKET_SIZE = 32 * 1024; /* 32 KB */
    protected final DatagramSocket socket;
    final ScheduledExecutorService executor;
    final int requestTimeout;
    final int retryDelay;
    private final DatagramRpcServer server;
    private final DatagramRpcClient client;
    private final Thread listenThread;

    public DatagramRpc(int port, int numberOfThreads, int requestTimeout, int retryDelay) throws SocketException {
        this(new DatagramSocket(port), numberOfThreads, requestTimeout, retryDelay);
    }

    DatagramRpc(int numberOfThreads, int requestTimeout, int retryDelay) throws SocketException {
        this(new DatagramSocket(), numberOfThreads, requestTimeout, retryDelay);
    }

    private DatagramRpc(DatagramSocket socket, int numberOfThreads, int requestTimeout, int retryDelay) throws SocketException {
        this.socket = socket;

        socket.setReceiveBufferSize(UDP_BUFFER_SIZE);
        socket.setSendBufferSize(UDP_BUFFER_SIZE);
        log.info("ReceiveBufferSize: {}, SendBufferSize: {}",
                socket.getReceiveBufferSize(), socket.getSendBufferSize());

        this.requestTimeout = requestTimeout;
        this.retryDelay = retryDelay;
        this.server = new DatagramRpcServer(this);
        this.client = new DatagramRpcClient(this);
        this.executor = Executors.newScheduledThreadPool(numberOfThreads);
        this.listenThread = new Thread(this::listen, "DatagramRpcListen");

        listenThread.start();
    }

    public int getPort() {
        return socket.getLocalPort();
    }

    public void shutdown() throws InterruptedException {
        executor.shutdown();
        socket.close();
        listenThread.join();
    }

    public DatagramRpcServer getServer() {
        return server;
    }

    public DatagramRpcClient getClient() {
        return client;
    }

    void sendPacket(DatagramPacket packet) {
        try {
            socket.send(packet);
        } catch (IOException e) {
            log.error(e);
        }
    }

    private void listen() {
        byte[] receiveBuffer = new byte[MAX_PACKET_SIZE];
        DatagramPacket packet = new DatagramPacket(receiveBuffer, MAX_PACKET_SIZE);
        while (!socket.isClosed()) {
            try {
                socket.receive(packet);
            } catch (IOException e) {
                if (socket.isClosed()) {
                    return;
                }
                log.error(e);
            }
            RpcMessage message;
            try {
                message = RpcMessage.parser().parseFrom(
                        packet.getData(), packet.getOffset(), packet.getLength()
                );
            } catch (InvalidProtocolBufferException e) {
                log.error("Unable to parse message packet");
                continue;
            }
            SocketAddress source = packet.getSocketAddress();
            try {
                executor.execute(() -> processMessage(message, source));
            } catch (RejectedExecutionException e) {
                return;
            }
        }
    }

    private void processMessage(RpcMessage message, SocketAddress source) {
        if (!DatagramRpcUtil.validate(message)) {
            return;
        }

        long id = message.getId();
        switch (message.getMessageType()) {
            case MESSAGE_REPLY:
                client.receiveReply(id, message.getPayload());
                break;
            case MESSAGE_REQUEST:
                server.receiveRequest(source, message);
                break;
            case MESSAGE_BEST_EFFORT:
                server.receiveBestEffortRequest(message);
                break;
            default:
                log.error("Invalid message type, id={}", id);
        }
    }
}
