package com.g10.cpen431.a12.keystore;

import com.g10.cpen431.RemapMessage;
import com.g10.cpen431.RpcMessage;
import com.g10.cpen431.a12.KeyValueStorage;
import com.g10.cpen431.a12.membership.MembershipService;
import com.g10.cpen431.a12.membership.PhysicalNode;
import com.g10.cpen431.a12.membership.ReplicaGroup;
import com.g10.cpen431.a12.membership.VirtualNode;
import com.g10.cpen431.a12.rpc.RpcService;
import com.google.protobuf.ByteString;
import lombok.extern.log4j.Log4j2;
import lombok.val;

import java.io.IOException;
import java.net.InetSocketAddress;

@Log4j2
public class KeyMigrator {

    public static void init() throws IOException {
        RpcService.registerHandler(RpcMessage.REMAP_FIELD_NUMBER, KeyMigrator::handleMessage);
        MembershipService.subscribePredecessorUpdate((x) -> remap(MembershipService.getReplicaGroup()));
        MembershipService.subscribeReplicaGroupUpdate((x) -> remap(MembershipService.getReplicaGroup()));
    }

    static synchronized void remap(ReplicaGroup primaryGroup) {
        log.info("remapping");

        int countKey = 0, countMessage = 0;
        for (val entry : KeyValueStorage.getInstance()) {
            byte[] key = entry.getKey().getArray();
            PhysicalNode primary = MembershipService.findPrimaryForKey(key);
            KeyValueStorage.Value value = entry.getValue();
            RpcMessage rpcMessage = RpcMessage.newBuilder().setRemap(
                    RemapMessage.newBuilder()
                            .setIsPrimary(primary != null)
                            .setKey(ByteString.copyFrom(key))
                            .setValue(ByteString.copyFrom(value.value))
                            .setVersion(value.version)
                            .setTimestamp(value.timestamp)
            ).build();
            if (primary == null) {
                for (VirtualNode vn : primaryGroup.getBackups()) {
                    sendMessage(vn.physicalNode.getTcpAddress(), rpcMessage);
                    countMessage += 1;
                }
            } else {
                sendMessage(primary.getTcpAddress(), rpcMessage);
                countMessage += 1;
            }
            countKey += 1;
        }
        log.info("remapped {} keys, sent {} messages", countKey, countMessage);
        RpcService.closeAllTcpConnections();
    }

    private static void sendMessage(InetSocketAddress target, RpcMessage message) {
        try {
            RpcService.sendMessageViaTcp(target, message);
        } catch (IOException e) {
            log.error(e);
        }
    }

    private static void handleMessage(RpcMessage rpcMessage) {
        RemapMessage message = rpcMessage.getRemap();

        if (message.getIsPrimary()) {
            PrimaryService.put(message.getKey().toByteArray(), message.getValue(),
                    message.getVersion(), message.getTimestamp());
        } else {
            KeyValueStorage.getInstance().put(message.getKey().toByteArray(),
                    message.getValue().asReadOnlyByteBuffer(), message.getVersion(), message.getTimestamp());
        }
    }
}
