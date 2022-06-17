package com.g10.cpen431.a12.rpc;

import com.g10.cpen431.RpcMessage;
import com.g10.util.Assertion;
import com.g10.util.SystemUtil;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A service used by other services for communication between server nodes.
 * <p>
 * It uses a number, named handle, to identify the type of each message.
 * Every {@link RpcMessage} includes a handle.
 * </p>
 * <p>
 * To send a message, use {@link #sendMessageViaUdp(SocketAddress, RpcMessage)} or
 * {@link #sendMessageViaTcp(SocketAddress, RpcMessage)}.
 * </p>
 * <p>
 * Upon receiving a message, the {@link RpcService} extracts the handle in the massage and calls the appropriate
 * handler function. To register a handler, use {@link #registerHandler(int, Consumer)}.
 * </p>
 */
@Log4j2
public class RpcService {
    private static final List<Consumer<RpcMessage>> handlers = new ArrayList<>();
    private static TcpCommunicator tcp;
    private static UdpCommunicator udp;

    public static void init(int udpPort, int tcpPort) throws IOException {
        tcp = new TcpCommunicator(tcpPort);
        udp = new UdpCommunicator(udpPort, SystemUtil.concurrencyLevel());
    }

    /**
     * Register the handler for a specific handle (type of message). There can be only one handler for each handle.
     *
     * @param handle type of message
     * @param messageHandler handler called when receiving messages
     */
    public static void registerHandler(int handle, Consumer<RpcMessage> messageHandler) {
        synchronized (handlers) {
            while (handle >= handlers.size()) {
                handlers.add(null);
            }
            Consumer<RpcMessage> oldHandler = handlers.set(handle, messageHandler);
            Assertion.check(oldHandler == null);
        }
    }

    /**
     * Send a message to the target host via UDP.
     *
     * @param target  the target address
     * @param message the message to send
     */
    public static void sendMessageViaUdp(SocketAddress target, RpcMessage message) {
        try {
            udp.sendMessage(target, message);
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Send a message to the target host via TCP. Open a new connection if necessary. The connection will be kept
     * alive for future use.
     *
     * @param target  address of the target host
     * @param message the message to send
     * @throws IOException if an I/O error occurs
     */
    public static void sendMessageViaTcp(SocketAddress target, RpcMessage message) throws IOException {
        tcp.sendMessage(target, message);
    }

    /**
     * Close all tcp connections initiated by this {@link RpcService}.
     */
    public static void closeAllTcpConnections() {
        tcp.closeAll();
    }

    /**
     * Handle a received message
     *
     * @param message the message received
     */
    static void receiveMessage(RpcMessage message) {
        try {
            int handle = message.getPayloadCase().getNumber();
            if (handle >= handlers.size()) {
                log.warn("handle {} does not exist.", handle);
                return;
            }
            handlers.get(handle).accept(message);
        } catch (RuntimeException e) {
            log.error(e);
        }
    }
}
