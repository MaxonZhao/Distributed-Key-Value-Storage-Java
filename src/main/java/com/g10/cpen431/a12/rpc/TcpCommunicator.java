package com.g10.cpen431.a12.rpc;

import com.g10.cpen431.RpcMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Sending and Receiving messages via TCP
 */
@Log4j2
class TcpCommunicator {
    private static final int TCP_BUFFER_SIZE = 64 * 1024 * 1024;

    private final ConcurrentHashMap<SocketAddress, Socket> localClients;
    private final ExecutorService executor;
    private final ServerSocket server;

    TcpCommunicator(int port) throws IOException {
        server = new ServerSocket(port);
        server.setReceiveBufferSize(TCP_BUFFER_SIZE);
        log.info("ReceiveBufferSize: {}", server.getReceiveBufferSize());

        localClients = new ConcurrentHashMap<>();
        executor = Executors.newCachedThreadPool();

        run();
    }

    /**
     * See {@link RpcService#sendMessageViaTcp(SocketAddress, RpcMessage)}.
     */
    void sendMessage(SocketAddress target, RpcMessage message) throws IOException {
        Socket socket = null;
        try {
            socket = getConnectionToRemoteServer(target);
            synchronized (socket) {
                message.writeDelimitedTo(socket.getOutputStream());
            }
        } catch (IOException e) {
            notifySocketDisconnected(target, socket);
            throw e;
        }
    }

    /**
     * Close all client sockets.
     */
    void closeAll() {
        for (SocketAddress address : localClients.keySet()) {
            Socket socket = localClients.remove(address);
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Get a TCP connection to the remote host. Open a new one if necessary.
     *
     * @param remoteAddress address of the target
     * @return a connected socket
     * @throws IOException if an I/O error occurs
     */
    private Socket getConnectionToRemoteServer(SocketAddress remoteAddress) throws IOException {
        Socket connection = localClients.get(remoteAddress);
        if (connection == null || connection.isClosed()) {
            /* There might be duplicate clients */ // TODO: lock
            connection = new Socket();
            connection.setTcpNoDelay(false);
            connection.setSendBufferSize(TCP_BUFFER_SIZE);
            connection.connect(remoteAddress, 200);
            localClients.put(remoteAddress, connection);
        }
        return connection;
    }

    private void notifySocketDisconnected(SocketAddress remoteAddress, Socket socket) {
        localClients.remove(remoteAddress, socket);
    }

    private void run() {
        new Thread(() -> {
            while (!server.isClosed()) {
                try {
                    Socket client = server.accept();
                    client.setReceiveBufferSize(TCP_BUFFER_SIZE);
                    executor.execute(() -> handleClient(client));
                } catch (IOException e) {
                    log.error(e);
                    return;
                }
            }
        }).start();
    }

    private void handleClient(Socket client) {
        while (!client.isClosed()) {
            RpcMessage message;
            try {
                message = RpcMessage.parseDelimitedFrom(client.getInputStream());
            } catch (InvalidProtocolBufferException e) {
                log.error("Unable to parse rpc request packet");
                continue;
            } catch (IOException e) {
                log.warn(e);
                break;
            }

            if (message == null) {
                break; /* EOF */
            }

            RpcService.receiveMessage(message);
        }
    }
}
