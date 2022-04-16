package com.g10.cpen431.a11.keystore;

import com.g10.cpen431.RemapMessage;
import com.g10.cpen431.a11.KeyValueStorage;
import com.g10.cpen431.a11.NodeInfo;
import com.g10.cpen431.a11.membership.MembershipService;
import com.g10.cpen431.a11.membership.PhysicalNode;
import com.g10.cpen431.a11.membership.ReplicaGroup;
import com.g10.cpen431.a11.membership.VirtualNode;
import com.g10.util.SystemUtil;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.log4j.Log4j2;
import lombok.val;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Log4j2
public class KeyMigrator {
    private static ServerSocket serverSocket;
    private static ExecutorService executor;

    public static void init() throws IOException {
        serverSocket = new ServerSocket(NodeInfo.getLocalPhysicalNode().getTcpAddress().getPort());
        serverSocket.setReceiveBufferSize(64 * 1024 * 1024);
        log.info("KeyMigrator ReceiveBufferSize: {}", serverSocket.getReceiveBufferSize());

        executor = Executors.newCachedThreadPool();

        run();
    }

    static synchronized void remap(ReplicaGroup primaryGroup) {
        if (SystemUtil.watchDogTick()) {
            return;
        }

        log.info("remapping");
        HashMap<Integer, Socket> connections = new HashMap<>();

        int countKey = 0, countMessage = 0, failedMessage = 0;
        for (val entry : KeyValueStorage.getInstance()) {
            byte[] key = entry.getKey().getArray();
            PhysicalNode primary = MembershipService.findPrimaryForKey(key);
            KeyValueStorage.Value value = entry.getValue();
            RemapMessage message = RemapMessage.newBuilder()
                    .setPrimary(primary != null)
                    .setKey(ByteString.copyFrom(key))
                    .setValue(ByteString.copyFrom(value.value))
                    .setVersion(value.version)
                    .setTimestamp(value.timestamp)
                    .build();
            if (primary == null) {
                for (VirtualNode vn : primaryGroup.getSuccessorList()) {
                    int physicalPort = vn.physicalNode.getTcpAddress().getPort();
                    try {
                        Socket connection = connections.get(physicalPort);
                        if (connection == null) {
                            connection = new Socket("127.0.0.1", physicalPort);
                            connections.put(physicalPort, connection);
                        }
                        message.writeDelimitedTo(connection.getOutputStream());
                    } catch (IOException e) {
                        failedMessage += 1;
                        log.error(e);
                    }
                    countMessage += 1;
                }
            } else {
                int physicalPort = primary.getTcpAddress().getPort();
                try {
                    Socket connection = connections.get(physicalPort);
                    if (connection == null) {
                        connection = new Socket("127.0.0.1", physicalPort);
                        connections.put(physicalPort, connection);
                    }
                    message.writeDelimitedTo(connection.getOutputStream());
                } catch (IOException e) {
                    failedMessage += 1;
                    log.error(e);
                }
                countMessage += 1;
            }
            countKey += 1;
        }
        log.info("remapped {} keys, sent {} messages, {} failed", countKey, countMessage, failedMessage);
        connections.forEach((integer, socket) -> {
            try {
                socket.close();
            } catch (IOException e) {
                log.error(e);
            }
        });
    }

    private static void run() {
        new Thread(() -> {
            while (!serverSocket.isClosed()) {
                try {
                    Socket client = serverSocket.accept();
                    executor.execute(() -> handleClient(client));
                } catch (IOException e) {
                    log.error(e);
                    return;
                }
            }
        }).start();
    }

    private static void handleClient(Socket client) {
        while (!client.isClosed()) {
            RemapMessage message;
            try {
                message = RemapMessage.parseDelimitedFrom(client.getInputStream());
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
                log.error("Unable to rpc request packet");
                continue;
            } catch (IOException e) {
                log.warn(e);
                break;
            }

            if (message == null) {
                break; // EOF
            }

            if (message.getPrimary()) {
                PrimaryService.put(message.getKey().toByteArray(), message.getValue(),
                        message.getVersion(), message.getTimestamp());
            } else {
                KeyValueStorage.getInstance().put(message.getKey().toByteArray(),
                        message.getValue().asReadOnlyByteBuffer(), message.getVersion(), message.getTimestamp());
            }
        }
    }
}
