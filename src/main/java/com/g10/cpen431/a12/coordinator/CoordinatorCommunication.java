package com.g10.cpen431.a12.coordinator;

import ca.NetSysLab.ProtocolBuffers.KeyValueRequest;
import com.g10.cpen431.RoutingMessage;
import com.g10.cpen431.RpcMessage;
import com.g10.cpen431.a12.membership.PhysicalNode;
import com.g10.cpen431.a12.rpc.RpcService;
import com.g10.util.ClientAddress;
import com.google.protobuf.ByteString;
import lombok.extern.log4j.Log4j2;

/**
 * Static methods for communication between coordinators
 */
@Log4j2
class CoordinatorCommunication {
    static void registerReceiveHandler() {
        RpcService.registerHandler(RpcMessage.ROUTING_FIELD_NUMBER, CoordinatorCommunication::handleMessage);
    }

    private static void handleMessage(RpcMessage rpcMessage) {
        RoutingMessage message = rpcMessage.getRouting();
        ClientAddress clientAddress = new ClientAddress(message.getClientIp(), message.getClientPort());
        Context context = new Context(message.getMessageId(), clientAddress, message.getRequest(), message.getTimestamp());
        CoordinatorHandlers.handle(context);
    }

    /**
     * Route a client request to the coordinator in another node
     *
     * @param target        target coordinator
     * @param messageId     message ID of the request
     * @param client        address of the client
     * @param clientRequest the client's request
     * @param timestamp     timestamp version of the value
     */
    static void sendRouting(PhysicalNode target, ByteString messageId, ClientAddress client,
                            KeyValueRequest clientRequest, long timestamp) {
        RpcMessage rpcMessage = RpcMessage.newBuilder().setRouting(
                RoutingMessage.newBuilder()
                        .setMessageId(messageId)
                        .setClientIp(client.getIp())
                        .setClientPort(client.getPort())
                        .setRequest(clientRequest)
                        .setTimestamp(timestamp)
        ).build();
        RpcService.sendMessageViaUdp(target.getUdpAddress(), rpcMessage);
    }
}
