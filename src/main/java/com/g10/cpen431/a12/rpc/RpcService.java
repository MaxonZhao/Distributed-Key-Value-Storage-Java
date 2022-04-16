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

@Log4j2
public class RpcService {
    private static final List<Consumer<RpcMessage>> handlers = new ArrayList<>();
    private static TcpCommunicator tcp;
    private static UdpCommunicator udp;

    public static void init(int udpPort, int tcpPort) throws IOException {
        tcp = new TcpCommunicator(tcpPort);
        udp = new UdpCommunicator(udpPort, SystemUtil.concurrencyLevel());
    }

    public static void registerHandler(int handle, Consumer<RpcMessage> messageHandler) {
        synchronized (handlers) {
            while (handle >= handlers.size()) {
                handlers.add(null);
            }
            Consumer<RpcMessage> oldHandler = handlers.set(handle, messageHandler);
            Assertion.check(oldHandler == null);
        }
    }

    public static void sendMessageViaUdp(SocketAddress target, RpcMessage message) {
        try {
            udp.sendMessage(target, message);
        } catch (IOException e) {
            log.error(e);
        }
    }

    public static void sendMessageViaTcp(SocketAddress target, RpcMessage message) throws IOException {
        tcp.sendMessage(target, message);
    }

    public static void closeAllTcpConnections() {
        tcp.closeAll();
    }

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
