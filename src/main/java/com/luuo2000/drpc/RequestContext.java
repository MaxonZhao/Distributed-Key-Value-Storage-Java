package com.luuo2000.drpc;

import com.google.protobuf.ByteString;
import com.luuo2000.util.Assertion;
import lombok.AllArgsConstructor;

import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

@AllArgsConstructor
public class RequestContext {
    private final DatagramRpc rpc;
    private final SocketAddress source;
    private final long id;
    private final int payloadType;
    private final ByteString payload;
    private final AtomicBoolean responseSent;

    RequestContext(DatagramRpc rpc, SocketAddress source, RpcMessage request) {
        this.rpc = rpc;
        this.source = source;
        this.id = request.getId();
        this.payloadType = request.getPayloadType();
        this.payload = request.getPayload();
        this.responseSent = new AtomicBoolean(false);
    }

    public ByteString getPayload() {
        return payload;
    }

    // TODO: test
    public void reply(ByteString reply) {
        sendResponse(reply);
    }

    public void acknowledge() {
        sendResponse(ByteString.EMPTY);
    }

    private void sendResponse(ByteString payload) {
        Assertion.check(source != null, "Unable to reply a best-effort request.");
        boolean responseSent = this.responseSent.getAndSet(true);
        Assertion.check(!responseSent, "Already sent a response to the client");

        byte[] message = DatagramRpcUtil.buildRpcMessage(id, rpc.requestTimeout,
                RpcMessage.MessageType.MESSAGE_REPLY, payloadType, payload);
        DatagramPacket packet = new DatagramPacket(message, message.length, source);
        rpc.sendPacket(packet);
    }
}
