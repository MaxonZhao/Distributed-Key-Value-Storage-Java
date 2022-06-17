package com.g10.cpen431.a12.keystore;

import com.g10.cpen431.BackupMessage;
import com.g10.cpen431.RpcMessage;
import com.g10.cpen431.a12.Command;
import com.g10.cpen431.a12.KeyValueStorage;
import com.g10.cpen431.a12.ServerException;
import com.g10.cpen431.a12.rpc.RpcService;
import com.google.protobuf.ByteString;
import com.matei.eece411.util.StringUtils;
import lombok.extern.log4j.Log4j2;

/**
 * The ReplicaService listens to messages sent by primary nodes and stores replicated key-value pairs.
 */
@Log4j2
public class ReplicaService {
    private static final KeyValueStorage storage = KeyValueStorage.getInstance();

    public static void init() {
        RpcService.registerHandler(RpcMessage.BACKUP_FIELD_NUMBER, ReplicaService::handleMessage);
    }

    private static void handleMessage(RpcMessage rpcMessage) {
        BackupMessage message = rpcMessage.getBackup();

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
        switch (command) {
            case PUT:
                ByteString value = message.getValue();
                if (value.size() == 0) {
                    log.error("Value is empty");
                    return;
                }
                storage.put(key, value.asReadOnlyByteBuffer(), message.getVersion(), message.getTimestamp());
                log.trace("Replica PUT key={}", () -> StringUtils.byteArrayToHexString(key));
                break;
            case REMOVE:
                storage.remove(key);
                log.trace("Replica REMOVE key={}", () -> StringUtils.byteArrayToHexString(key));
                break;
            default:
                log.error("Invalid replica command");
        }
    }
}
