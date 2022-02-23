package com.g10.cpen431.a7;

import ca.NetSysLab.ProtocolBuffers.KeyValueRequest;
import ca.NetSysLab.ProtocolBuffers.KeyValueResponse;
import com.g10.util.*;
import com.g10.util.Process;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.matei.eece411.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketAddress;

public class KeyValueStorageServer implements RequestReplyApplication {
    private static final Logger logger = LogManager.getLogger(KeyValueStorageServer.class);

    private static final int DEFAULT_PORT = 16589;

    private static final Reply outOfMemoryReply = new Reply(ErrCode.OUT_OF_SPACE, true);
    private static final Reply internalFailureReply = new Reply(ErrCode.INTERNAL_FAILURE, true);
    private static final Reply simpleSuccess = new Reply(ErrCode.SUCCESS, false);

    private final RequestReplyServer transportLayer;
    private final HashCircle hashCircle;
    private final KeyValueStorage dataModel;

    public KeyValueStorageServer(String serverListPath, KeyValueStorage dataModel) throws IOException {
        this(serverListPath, dataModel, DEFAULT_PORT);
    }

    public KeyValueStorageServer(String serverListPath, KeyValueStorage dataModel, int port) throws IOException {
        this.transportLayer = new RequestReplyServer(port, this);
        this.dataModel = dataModel;
        this.hashCircle = HashCircle.getInstance(serverListPath);
    }

    private static void handleShutdown(KeyValueRequest.KVRequest request) throws ServerException {
        extractAndCheck(request, false, false, false);

        System.exit(0);
        throw new RuntimeException("System.exit(0) returned.");
    }

    private static Reply handleIsAlive(KeyValueRequest.KVRequest request) throws ServerException {
        extractAndCheck(request, false, false, false);

        return simpleSuccess;
    }

    private static Reply handleGetPid(KeyValueRequest.KVRequest request) throws ServerException {
        extractAndCheck(request, false, false, false);

        int pid = Process.getProcessId();

        KeyValueResponse.KVResponse response = KeyValueResponse.KVResponse.newBuilder()
                .setErrCode(ErrCode.SUCCESS.val)
                .setPid(pid)
                .build();
        return new Reply(response, true);
    }

    private static Reply handleGetMembershipCount(KeyValueRequest.KVRequest request)
            throws ServerException {
        extractAndCheck(request, false, false, false);

        KeyValueResponse.KVResponse response = KeyValueResponse.KVResponse.newBuilder()
                .setErrCode(ErrCode.SUCCESS.val)
                .setMembershipCount(1)
                .build();
        return new Reply(response, true);
    }

    private static Triple<byte[], ByteString, Integer> extractAndCheck(
            KeyValueRequest.KVRequest request, boolean hasKey, boolean hasValue, boolean hasVersion)
            throws ServerException {
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

    public void run() throws IOException {
        transportLayer.run();
    }

    public void close() {
        transportLayer.close();
    }

    @Override
    public Reply handleRequest(InputStream rawRequest) {
        try {
            KeyValueRequest.KVRequest request = KeyValueRequest.KVRequest.parseFrom(rawRequest);
            return handleGeneralRequest(request);
        } catch (InvalidProtocolBufferException e) {
            logger.warn("Received an invalid request.");
            return new Reply(ErrCode.MALFORMED_REQUEST, true);
        } catch (ServerException e) {
            return new Reply(e.getErrCode(), false);
        } catch (IOException e) {
            logger.error("IOException caught. {}", e::toString);
            return internalFailureReply;
        } catch (OutOfMemoryError e) {
            logger.error("OutOfMemoryError caught. Server might fail. {}", e::toString);
            return outOfMemoryReply;
        } catch (RuntimeException e) {
            logger.fatal("RuntimeException caught. Server might fail. {}", e::toString);
            return internalFailureReply;
        }
    }

    private Reply handleGeneralRequest(KeyValueRequest.KVRequest request) throws ServerException {
        Command command = Command.get(request.getCommand());
        logger.info("New command received. Command: {}", command);
        switch (command) {
            case PUT:
                return handlePut(request);
            case GET:
                return handleGet(request);
            case REMOVE:
                return handleRemove(request);
            case SHUTDOWN:
                handleShutdown(request);
            case WIPE_OUT:
                return handleWipeOut(request);
            case IS_ALIVE:
                return handleIsAlive(request);
            case GET_PID:
                return handleGetPid(request);
            case GET_MEMBERSHIP_COUNT:
                return handleGetMembershipCount(request);
            default:
                throw new UnsupportedOperationException();
        }
    }

    private Reply handlePut(KeyValueRequest.KVRequest request) throws ServerException {
        Triple<byte[], ByteString, Integer> parameters =
                extractAndCheck(request, true, true, true);

        /* Check if routing is needed */
        SocketAddress target = hashCircle.findNodeFromHash(parameters.first);
        if (target != null) {
            return new RequestReplyApplication.Reply(target);
        }

        if (SystemUtil.checkMemoryStress()) {
            throw new ServerException(ErrCode.OUT_OF_SPACE);
        }

        logger.info(() -> String.format("PUT - Key: %s, Value: %s, Version: %d",
                StringUtils.byteArrayToHexString(parameters.first),
                ByteUtil.bytesToHexString(parameters.second),
                parameters.third));

        boolean success = dataModel.put(parameters.first,
                parameters.second.asReadOnlyByteBuffer(), parameters.third);
        if (!success)
            throw new ServerException(ErrCode.OUT_OF_SPACE);

        return simpleSuccess;
    }

    private Reply handleGet(KeyValueRequest.KVRequest request) throws ServerException {
        Triple<byte[], ByteString, Integer> parameters =
                extractAndCheck(request, true, false, false);

        /* Check if routing is needed */
        SocketAddress target = hashCircle.findNodeFromHash(parameters.first);
        if (target != null) {
            return new RequestReplyApplication.Reply(target);
        }

        Tuple<byte[], Integer> result = dataModel.get(parameters.first);

        if (result == null) {
            logger.info("GET - Key: {}, Value: null, Version: null",
                    () -> StringUtils.byteArrayToHexString(parameters.first));
            throw new ServerException(ErrCode.UNKNOWN_KEY);
        } else {
            logger.info(() -> String.format("GET - Key: %s, Value: %s, Version: %d",
                    StringUtils.byteArrayToHexString(parameters.first),
                    StringUtils.byteArrayToHexString(result.first),
                    result.second));
        }

        KeyValueResponse.KVResponse response = KeyValueResponse.KVResponse.newBuilder()
                .setErrCode(ErrCode.SUCCESS.val)
                .setValue(ByteString.copyFrom(result.first)) // TODO: optimize
                .setVersion(result.second)
                .build();
        return new Reply(response.toByteString(), true);
    }

    private Reply handleRemove(KeyValueRequest.KVRequest request) throws ServerException {
        Triple<byte[], ByteString, Integer> parameters =
                extractAndCheck(request, true, false, false);

        /* Check if routing is needed */
        SocketAddress target = hashCircle.findNodeFromHash(parameters.first);
        if (target != null) {
            return new RequestReplyApplication.Reply(target);
        }

        boolean result = dataModel.remove(parameters.first);
        if (!result)
            throw new ServerException(ErrCode.UNKNOWN_KEY);

        return simpleSuccess;
    }

    private Reply handleWipeOut(KeyValueRequest.KVRequest request) throws ServerException {
        extractAndCheck(request, false, false, false);

        dataModel.wipeOut();
        return simpleSuccess;
    }
}
