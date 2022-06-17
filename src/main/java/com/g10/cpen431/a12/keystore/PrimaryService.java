package com.g10.cpen431.a12.keystore;

import com.g10.cpen431.BackupMessage;
import com.g10.cpen431.RpcMessage;
import com.g10.cpen431.a12.Command;
import com.g10.cpen431.a12.KeyValueStorage;
import com.g10.cpen431.a12.membership.MembershipService;
import com.g10.cpen431.a12.membership.PhysicalNode;
import com.g10.cpen431.a12.membership.VirtualNode;
import com.g10.cpen431.a12.rpc.RpcService;
import com.google.protobuf.ByteString;
import com.google.protobuf.UnsafeByteOperations;
import com.matei.eece411.util.StringUtils;
import lombok.extern.log4j.Log4j2;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * The PrimaryService serves requests to the primary node in the primary-backup protocol.
 */
@Log4j2
public class PrimaryService {
    private static final KeyValueStorage storage = KeyValueStorage.getInstance();

    public static void init() {
        log.info("Initial primaryGroup: {}", MembershipService.getReplicaGroup());
    }

    /**
     * Serve a GET request.
     *
     * @param key the requested key
     * @return if the key exists, return the associated value. Otherwise, return null.
     */
    @Nullable
    public static KeyValueStorage.Value get(byte[] key) {
        return storage.get(key);
    }

    /**
     * Serve a PUT request. This method also forwards the request to the replicas.
     *
     * @param key the key
     * @param value the value associated with the key
     * @param version the version of the key-value pair
     * @param timestamp the timestamp associated with the value
     */
    public static void put(byte[] key, ByteString value, int version, long timestamp) {
        log.trace("Primary PUT key={}", () -> StringUtils.byteArrayToHexString(key));
        storage.put(key, value.asReadOnlyByteBuffer(), version, timestamp);

        RpcMessage rpcMessage = RpcMessage.newBuilder().setBackup(BackupMessage.newBuilder()
                .setCommand(Command.PUT.val)
                .setKey(UnsafeByteOperations.unsafeWrap(key))
                .setValue(value)
                .setVersion(version)
                .setTimestamp(timestamp)
        ).build();
        for (VirtualNode virtualNode : MembershipService.getReplicaGroup().getBackups()) {
            sendBackupKey(virtualNode.physicalNode, rpcMessage);
        }
    }

    /**
     * Serve a REMOVE request. This method also forwards the request to the replicas.
     *
     * @param key the key to remove
     * @return true the remove request is successfully executed, false if the key does not exist.
     */
    public static boolean remove(byte[] key) {
        log.trace("Primary REMOVE key={}", () -> StringUtils.byteArrayToHexString(key));
        boolean success = storage.remove(key);
        if (!success) {
            return false;
        }

        RpcMessage rpcMessage = RpcMessage.newBuilder().setBackup(
                BackupMessage.newBuilder()
                        .setCommand(Command.REMOVE.val)
                        .setKey(UnsafeByteOperations.unsafeWrap(key))
        ).build();

        for (VirtualNode virtualNode : MembershipService.getReplicaGroup().getBackups()) {
            sendBackupKey(virtualNode.physicalNode, rpcMessage);
        }

        return true;
    }

    private static void sendBackupKey(PhysicalNode target, RpcMessage message) {
        try {
            RpcService.sendMessageViaTcp(target.getTcpAddress(), message);
        } catch (IOException e) {
            log.error(e);
        }
    }
}
