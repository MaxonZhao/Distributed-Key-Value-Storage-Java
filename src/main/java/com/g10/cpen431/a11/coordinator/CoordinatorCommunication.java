package com.g10.cpen431.a11.coordinator;

import ca.NetSysLab.ProtocolBuffers.KeyValueRequest;
import com.g10.cpen431.RoutingMessage;
import com.g10.cpen431.RpcPayloadType;
import com.g10.cpen431.a11.membership.PhysicalNode;
import com.g10.cpen431.a11.rpc.RpcService;
import com.g10.util.ClientAddress;
import com.google.protobuf.ByteString;
import lombok.extern.log4j.Log4j2;

/**
 * Static methods for communication between coordinators
 */
@Log4j2
class CoordinatorCommunication {
    static void registerReceiveHandler() {
        RpcService.getServer().registerHandler(RpcPayloadType.ROUTING_VALUE, rpcContext -> {
            try {
                RoutingMessage message = RoutingMessage.parseFrom(rpcContext.getPayload());
                handleMessage(message);
            } catch (Throwable e) {
                log.error(e);
            }
        });
    }

    private static void handleMessage(RoutingMessage message) {
        ClientAddress clientAddress = new ClientAddress(message.getClientIp(), message.getClientPort());
        Context context = new Context(message.getMessageId(), clientAddress, message.getRequest(), message.getTimestamp());
        CoordinatorHandlers.handle(context);
    }

    /**
     * Route a client request to another coordinator
     *
     * @param target        target coordinator
     * @param messageId     message ID of the request
     * @param client        address of the client
     * @param clientRequest the client's request
     * @param timestamp
     */
    static void sendRouting(PhysicalNode target, ByteString messageId, ClientAddress client,
                            KeyValueRequest clientRequest, long timestamp) {
        ByteString message = RoutingMessage.newBuilder()
                .setMessageId(messageId)
                .setClientIp(client.getIp())
                .setClientPort(client.getPort())
                .setRequest(clientRequest)
                .setTimestamp(timestamp)
                .build().toByteString();
        RpcService.getClient().sendMessageUnreliably(target.getRpcAddress(), RpcPayloadType.ROUTING_VALUE, message);
    }
}
