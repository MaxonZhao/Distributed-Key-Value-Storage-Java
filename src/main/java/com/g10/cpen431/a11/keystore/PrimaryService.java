package com.g10.cpen431.a11.keystore;

import com.g10.cpen431.KeyStoreMessage;
import com.g10.cpen431.RpcPayloadType;
import com.g10.cpen431.a11.Command;
import com.g10.cpen431.a11.KeyValueStorage;
import com.g10.cpen431.a11.ServerException;
import com.g10.cpen431.a11.membership.MembershipService;
import com.g10.cpen431.a11.membership.PhysicalNode;
import com.g10.cpen431.a11.membership.ReplicaGroup;
import com.g10.cpen431.a11.membership.VirtualNode;
import com.g10.cpen431.a11.rpc.RpcService;
import com.g10.util.SystemUtil;
import com.g10.util.Triple;
import com.g10.util.Tuple;
import com.google.protobuf.ByteString;
import com.matei.eece411.util.StringUtils;
import lombok.extern.log4j.Log4j2;
import lombok.val;

import java.util.List;


@Log4j2
public class PrimaryService {
    private static final KeyValueStorage storage = KeyValueStorage.getInstance();
    private static volatile ReplicaGroup primaryGroup;

    public static void init() {
        primaryGroup = MembershipService.getPrimaryReplicaGroup();
        log.info("Initial primaryGroup: {}", primaryGroup);
        registerReceiveHandler();

        MembershipService.subscribePredecessorUpdate(PrimaryService::handlePredecessorUpdate);
//        MembershipService.subscribeNodeJoin(PrimaryService::handleNodeJoin);
        MembershipService.subscribeSuccessorList(PrimaryService::handlePrimaryReplicaGroupUpdate);
    }

    public static KeyValueStorage.Value get(byte[] key) {
        return storage.get(key);
    }

    public static void put(byte[] key, ByteString value, int version, long timestamp) {
        log.trace("Primary PUT key={}", () -> StringUtils.byteArrayToHexString(key));
        storage.put(key, value.asReadOnlyByteBuffer(), version, timestamp);

        if (primaryGroup.getSuccessorList().size() == 0)
            return;
        KeyStoreMessage replicaMessage = KeyStoreMessage.newBuilder()
                .setCommand(Command.PUT.val)
                .setKey(ByteString.copyFrom(key))
                .setValue(value)
                .setVersion(version)
                .setTimestamp(timestamp)
                .build();
        for (VirtualNode virtualNode : primaryGroup.getSuccessorList()) {
            sendBackupKey(virtualNode.physicalNode, replicaMessage);
        }
    }

    public static boolean remove(byte[] key) {
        log.trace("Primary REMOVE key={}", () -> StringUtils.byteArrayToHexString(key));
        boolean success = storage.remove(key);
        if (!success) {
            return false;
        }

        if (primaryGroup.getSuccessorList().size() == 0)
            return true;
        KeyStoreMessage replicaMessage = KeyStoreMessage.newBuilder()
                .setCommand(Command.REMOVE.val)
                .setKey(ByteString.copyFrom(key))
                .build();
        for (VirtualNode virtualNode : primaryGroup.getSuccessorList()) {
            sendBackupKey(virtualNode.physicalNode, replicaMessage);
        }

        return true;
    }

    private static void registerReceiveHandler() {
        RpcService.getServer().registerHandler(RpcPayloadType.PRIMARY_VALUE, rpcContext -> {
            try {
                KeyStoreMessage message = KeyStoreMessage.parseFrom(rpcContext.getPayload());

                handleMessage(message);
                rpcContext.acknowledge();
            } catch (Throwable e) {
                log.error(e);
            }
        });
    }

    private static void handleMessage(KeyStoreMessage message) {
        Command command;
        try {
            command = Command.get(message.getCommand());
        } catch (ServerException e) {
            log.error("Unknown replica command");
            return;
        }

        byte[] key = message.getKey().toByteArray();
        if (key.length == 0) {
            log.error("Key is empty");
            return;
        }
//        if (MembershipService.findPrimaryForKey(key) != null) {
//            return;
//        }
        switch (command) {
            case PUT:
                ByteString value = message.getValue();
                if (value.size() == 0) {
                    log.error("Value is empty");
                    return;
                }
                put(key, value, message.getVersion(), message.getTimestamp());
                log.trace("Primary PUT key={}", () -> StringUtils.byteArrayToHexString(key));
                break;
            case REMOVE:
                remove(key);
                log.trace("Primary REMOVE key={}", () -> StringUtils.byteArrayToHexString(key));
                break;
            default:
                log.error("Invalid replica command");
        }
    }

    private static void sendBackupKey(PhysicalNode target, KeyStoreMessage message) {
        RpcService.getClient()
                .sendMessageReliably(target.getRpcAddress(), RpcPayloadType.BACKUP_VALUE, message.toByteString());
    }

    private static void sendPrimaryKey(PhysicalNode target, KeyStoreMessage message) {
        RpcService.getClient()
                .sendMessageReliably(target.getRpcAddress(), RpcPayloadType.PRIMARY_VALUE, message.toByteString());
    }

    private static void handlePredecessorUpdate(VirtualNode newPredecessor) {
        KeyMigrator.remap(primaryGroup);
    }

//    private static void handleNodeJoin(List<Triple<Integer, Long, Long>> x) {
//        remap();
//    }

    private static void handlePrimaryReplicaGroupUpdate(ReplicaGroup newGroup) {
        // TODO: timestamp
        primaryGroup = newGroup;
        KeyMigrator.remap(primaryGroup);
    }
}
