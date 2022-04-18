package com.g10.cpen431.a11.keystore;

import com.g10.cpen431.KeyStoreMessage;
import com.g10.cpen431.RpcPayloadType;
import com.g10.cpen431.a11.Command;
import com.g10.cpen431.a11.KeyValueStorage;
import com.g10.cpen431.a11.ServerException;
import com.g10.cpen431.a11.rpc.RpcService;
import com.google.protobuf.ByteString;
import com.matei.eece411.util.StringUtils;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ReplicaService {
    private static final KeyValueStorage storage = KeyValueStorage.getInstance();

    public static void init() {
        registerReceiveHandler();
    }

    private static void registerReceiveHandler() {
        RpcService.getServer().registerHandler(RpcPayloadType.BACKUP_VALUE, rpcContext -> {
            try {
                KeyStoreMessage message = KeyStoreMessage.parseFrom(rpcContext.getPayload());

                handleMessage(message);
                rpcContext.acknowledge();
            } catch (Throwable e) {
                log.error(e);
            }
        });
    }

    public static void handleMessage(KeyStoreMessage message) {
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