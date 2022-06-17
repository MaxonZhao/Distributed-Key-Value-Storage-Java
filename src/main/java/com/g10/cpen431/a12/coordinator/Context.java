package com.g10.cpen431.a12.coordinator;

import ca.NetSysLab.ProtocolBuffers.KeyValueRequest;
import ca.NetSysLab.ProtocolBuffers.KeyValueResponse;
import com.g10.cpen431.a12.membership.PhysicalNode;
import com.g10.util.Assertion;
import com.g10.util.ClientAddress;
import com.g10.util.SystemUtil;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;

/**
 * Holds information relevant to the current request/reply.
 */
@Log4j2
class Context {
    private static ClientCommunicator clientComm;

    private final ByteString messageId;
    private final ClientAddress clientAddress;
    private final ByteString rawRequest;
    private final long versionTimestamp;
    private KeyValueRequest kvRequest;

    Context(ByteString messageId, ClientAddress clientAddress,
            ByteString rawRequest, KeyValueRequest kvRequest, long versionTimestamp) {
        Assertion.check(clientComm != null);
        this.rawRequest = rawRequest;
        this.messageId = messageId;
        this.clientAddress = clientAddress;
        this.kvRequest = kvRequest;
        this.versionTimestamp = versionTimestamp;
    }

    Context(ByteString messageId, ClientAddress clientAddress, ByteString rawRequest) {
        this(messageId, clientAddress, rawRequest, null, SystemUtil.generateTimestampVersion());
    }

    Context(ByteString messageId, ClientAddress clientAddress, KeyValueRequest kvRequest, long versionTimestamp) {
        this(messageId, clientAddress, null, kvRequest, versionTimestamp);
    }

    static void init(ClientCommunicator clientComm) {
        Context.clientComm = clientComm;
    }

    /**
     * @throws InvalidProtocolBufferException if the request is malformed
     * @throws IOException                    unknown IO error
     */
    void parseRequestIfNeeded() throws IOException {
        if (kvRequest == null) {
            kvRequest = KeyValueRequest.parseFrom(rawRequest.newInput());
        }
    }

    KeyValueRequest getRequest() {
        if (kvRequest == null) {
            throw new AssertionError("parseRequest() must be called first.");
        }
        return kvRequest;
    }

    ByteString getMessageId() {
        return messageId;
    }

    long getVersionTimestamp() {
        return versionTimestamp;
    }

    /**
     * Send a response to the client.
     *
     * @param response   the response
     * @param idempotent true if this response does not need to be cached, false otherwise.
     */
    void replyToClient(KeyValueResponse response, boolean idempotent) {
        try {
            clientComm.replyToClient(messageId, clientAddress, response, idempotent);
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Send an idempotent response to the client.
     */
    void replyToClient(KeyValueResponse response) {
        replyToClient(response, true);
    }

    /**
     * Send an idempotent response to the client.
     */
    void replyToClient(KeyValueResponse.Builder responseBuilder) {
        replyToClient(responseBuilder.build());
    }

    /**
     * Send a message to the client.
     */
    void replyToClient(byte[] message) {
        try {
            clientComm.replyToClient(message, clientAddress);
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Route this request to another server node.
     *
     * @param target target server node
     */
    void routeToNode(PhysicalNode target) {
        CoordinatorCommunication.sendRouting(target, messageId, clientAddress, kvRequest, versionTimestamp);
    }
}
