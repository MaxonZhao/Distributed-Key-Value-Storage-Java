package com.g10.cpen431.a11.coordinator;

import ca.NetSysLab.ProtocolBuffers.KeyValueRequest;
import ca.NetSysLab.ProtocolBuffers.KeyValueResponse;
import com.g10.cpen431.a11.Command;
import com.g10.cpen431.a11.ErrCode;
import com.g10.cpen431.a11.KeyValueStorage;
import com.g10.cpen431.a11.ServerException;
import com.g10.cpen431.a11.keystore.PrimaryService;
import com.g10.cpen431.a11.membership.MembershipService;
import com.g10.cpen431.a11.membership.PhysicalNode;
import com.g10.util.*;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.matei.eece411.util.StringUtils;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.time.Duration;

@Log4j2
class CoordinatorHandlers {
    private static final Duration CACHE_TIMEOUT = Duration.ofSeconds(1);
    public static final Cache<ByteString, byte[]> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(CACHE_TIMEOUT)
            .concurrencyLevel(SystemUtil.concurrencyLevel())
            .build();
    /* don't use max size of weight */ // TODO: explain
    /* NOTE: memory stress of cache handled in CoordinatorService */

    private static final KeyValueResponse outOfMemoryReply = kvResponseBuilder(ErrCode.OUT_OF_SPACE).build();
    private static final KeyValueResponse internalFailureReply = kvResponseBuilder(ErrCode.INTERNAL_FAILURE).build();
    private static final KeyValueResponse successReply = kvResponseBuilder().build();

    static void handle(Context context) {
        try {
            context.parseRequestIfNeeded();
        } catch (InvalidProtocolBufferException e) {
            log.warn("Received an invalid request.");
            context.replyToClient(kvResponseBuilder(ErrCode.MALFORMED_REQUEST).build());
            return;
        } catch (IOException e) {
            log.error("IOException caught. {}", e::toString);
            context.replyToClient(internalFailureReply);
            return;
        }

        try {
            byte[] cachedClientMessage = cache.getIfPresent(context.getMessageId());
            if (cachedClientMessage != null) {
                context.replyToClient(cachedClientMessage);
            } else {
                handleGeneralRequest(context);
            }
        } catch (ServerException e) {
            context.replyToClient(kvResponseBuilder(e.getErrCode()).build());
        } catch (OutOfMemoryError e) {
            log.error("OutOfMemoryError caught. Server might fail. {}", e::toString);
            context.replyToClient(outOfMemoryReply);
        } catch (RuntimeException e) {
            log.fatal("RuntimeException caught. Server might fail. {} {}", e::toString, e::getStackTrace);
            context.replyToClient(internalFailureReply);
        }
    }

    private static void handleGeneralRequest(Context context) throws ServerException {
        Command command = Command.get(context.getRequest().getCommand());
        log.trace("New command received. Command: {}", command);
        switch (command) {
            case PUT:
                handlePut(context);
                break;
            case GET:
                handleGet(context);
                break;
            case REMOVE:
                handleRemove(context);
                break;
            case SHUTDOWN:
                handleShutdown(context);
                break;
            case WIPE_OUT:
                handleWipeOut(context);
                break;
            case IS_ALIVE:
                handleIsAlive(context);
                break;
            case GET_PID:
                handleGetPid(context);
                break;
            case GET_MEMBERSHIP_COUNT:
                handleGetMembershipCount(context);
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    private static Triple<byte[], ByteString, Integer> extractAndCheck(
            Context context, boolean hasKey, boolean hasValue, boolean hasVersion)
            throws ServerException {
        KeyValueRequest request = context.getRequest();
        byte[] key = request.getKey().toByteArray();
        ByteString value = request.getValue();
        int version = request.getVersion();

        if ((hasKey && key.length == 0) || (!hasKey && key.length != 0) || key.length > 32)
            throw new ServerException(ErrCode.INVALID_KEY);

        if ((hasValue && value.size() == 0) || (!hasValue && value.size() != 0) || value.size() > 10000)
            throw new ServerException(ErrCode.INVALID_VALUE);

        /* Version is not checked */

        return new Triple<>(key, value, version);
    }

    private static KeyValueResponse.Builder kvResponseBuilder() {
        return kvResponseBuilder(ErrCode.SUCCESS);
    }

    private static KeyValueResponse.Builder kvResponseBuilder(ErrCode errCode) {
        return KeyValueResponse.newBuilder()
                .setErrCode(errCode.val);
    }

    private static void handleShutdown(Context context) throws ServerException {
        extractAndCheck(context, false, false, false);

        System.exit(0);
        throw new RuntimeException("System.exit(0) returned.");
    }

    private static void handleIsAlive(Context context) throws ServerException {
        extractAndCheck(context, false, false, false);

        context.replyToClient(successReply);
    }

    private static void handleGetPid(Context context) throws ServerException {
        extractAndCheck(context, false, false, false);

        int pid = SystemUtil.getProcessId();
        context.replyToClient(kvResponseBuilder().setPid(pid));
    }

    private static void handleGetMembershipCount(Context context)
            throws ServerException {
        extractAndCheck(context, false, false, false);

        context.replyToClient(
                kvResponseBuilder(ErrCode.SUCCESS)
                        .setMembershipCount(MembershipService.getNumOfAliveNodes())
                        .build()
        );
    }

    private static void handlePut(Context context) throws ServerException {
        Triple<byte[], ByteString, Integer> parameters =
                extractAndCheck(context, true, true, true);

        /* Check if routing is needed */
        PhysicalNode target = MembershipService.findPrimaryForKey(parameters.first);
        if (target != null) {
            context.routeToNode(target);
            return;
        }

        if (MemoryManager.checkMemoryStress()) {
            throw new ServerException(ErrCode.OUT_OF_SPACE);
        }

        log.trace(() -> String.format("PUT - Key: %s, Value: %s, Version: %d",
                StringUtils.byteArrayToHexString(parameters.first),
                ByteUtil.bytesToHexString(parameters.second),
                parameters.third));

        long timestamp = context.getVersionTimestamp();
        PrimaryService.put(parameters.first, parameters.second, parameters.third, timestamp);

        context.replyToClient(successReply);
    }

    private static void handleGet(Context context) throws ServerException {
        Triple<byte[], ByteString, Integer> parameters =
                extractAndCheck(context, true, false, false);

        /* Check if routing is needed */
        PhysicalNode target = MembershipService.findPrimaryForKey(parameters.first);
        if (target != null) {
            context.routeToNode(target);
            return;
        }

        KeyValueStorage.Value result = PrimaryService.get(parameters.first);

        if (result == null) {
            log.trace("GET - Key: {}, Value: null, Version: null",
                    () -> StringUtils.byteArrayToHexString(parameters.first));
            throw new ServerException(ErrCode.UNKNOWN_KEY);
        } else {
            log.trace(() -> String.format("GET - Key: %s, Value: %s, Version: %d",
                    StringUtils.byteArrayToHexString(parameters.first),
                    StringUtils.byteArrayToHexString(result.value),
                    result.version));
        }

        context.replyToClient(kvResponseBuilder()
                .setValue(ByteString.copyFrom(result.value)) // TODO: optimize
                .setVersion(result.version)
        );
    }

    private static void handleRemove(Context context) throws ServerException {
        Triple<byte[], ByteString, Integer> parameters =
                extractAndCheck(context, true, false, false);

        /* Check if routing is needed */
        PhysicalNode target = MembershipService.findPrimaryForKey(parameters.first);
        if (target != null) {
            context.routeToNode(target);
            return;
        }

        boolean result = PrimaryService.remove(parameters.first);
        if (result) {
            context.replyToClient(successReply, false);
        } else {
            context.replyToClient(kvResponseBuilder(ErrCode.UNKNOWN_KEY).build(), false);
        }
    }

    private static void handleWipeOut(Context context) throws ServerException {
        extractAndCheck(context, false, false, false);

        KeyValueStorage.getInstance().wipeOut();
        context.replyToClient(successReply);
    }
}
