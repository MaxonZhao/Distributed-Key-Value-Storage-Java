package com.luuo2000.drpc;

import com.luuo2000.util.Assertion;
import lombok.extern.log4j.Log4j2;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Log4j2
public class DatagramRpcServer {
    private final DatagramRpc rpc;
    private final List<Consumer<RequestContext>> handlers;

    DatagramRpcServer(DatagramRpc rpc) {
        this.rpc = rpc;
        this.handlers = new ArrayList<>();
    }

    /**
     * If the network packet is duplicated, the same message may be delivered multiple times.
     * TODO: add cache to fix this
     */
    public void registerHandler(int handle, Consumer<RequestContext> requestHandler) {
        synchronized (handlers) {
            while (handle >= handlers.size()) {
                handlers.add(null);
            }
            Consumer<RequestContext> oldHandler = handlers.set(handle, requestHandler);
            Assertion.check(oldHandler == null);
        }
    }

    void receiveRequest(SocketAddress source, RpcMessage request) {
        int handle = request.getPayloadType();
        handlers.get(handle).accept(new RequestContext(rpc, source, request));
    }

    void receiveBestEffortRequest(RpcMessage request) {
        receiveRequest(null, request);
    }
}