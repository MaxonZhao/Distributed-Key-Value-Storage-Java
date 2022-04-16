package com.luuo2000.drpc;

import com.google.protobuf.ByteString;
import com.luuo2000.util.Assertion;
import lombok.extern.log4j.Log4j2;

import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Log4j2
public final class DatagramRpcClient {
    private final DatagramRpc rpc;
    private final ConcurrentHashMap<Long, RequestRecord> requestContexts;

    DatagramRpcClient(DatagramRpc rpc) {
        this.rpc = rpc;
        this.requestContexts = new ConcurrentHashMap<>();
    }

    // TODO: Test callback
    public void call(SocketAddress target, int payloadType, ByteString payload, Consumer<ByteString> callback) {
        long id = DatagramRpcUtil.generateId();
        byte[] message = DatagramRpcUtil.buildRpcMessage(id, rpc.requestTimeout,
                RpcMessage.MessageType.MESSAGE_REQUEST, payloadType, payload);
        DatagramPacket packet = new DatagramPacket(message, message.length, target);
        scheduleRequest(id, packet, callback);
    }

    public void sendMessageReliably(SocketAddress target, int payloadType, ByteString payload) {
        call(target, payloadType, payload, null);
    }

    public void sendMessageUnreliably(SocketAddress target, int payloadType, ByteString payload) {
        long id = DatagramRpcUtil.generateId();
        byte[] message = DatagramRpcUtil.buildRpcMessage(id, rpc.requestTimeout,
                RpcMessage.MessageType.MESSAGE_BEST_EFFORT, payloadType, payload);
        DatagramPacket packet = new DatagramPacket(message, message.length, target);
        rpc.sendPacket(packet);
    }

    void receiveReply(long id, ByteString payload) {
        RequestRecord context = requestContexts.get(id);
        if (context != null) {
            context.receiveReply(payload);
        }
    }

    private void scheduleRequest(long id, DatagramPacket packet, Consumer<ByteString> callback) {
        RequestRecord record = new RequestRecord(id, callback);

        Future<?> sendFuture = rpc.executor.scheduleWithFixedDelay(() -> {
            if (record.checkExpire()) {
                return;
            }
            rpc.sendPacket(packet);
        }, 0, rpc.retryDelay, TimeUnit.MILLISECONDS);
        // TODO: If the reply arrives at this point, the sendFuture task will not be cancelled.
        record.setSendFuture(sendFuture);
    }

    private final class RequestRecord {
        private final Long id;
        private final long expiry;
        private final AtomicReference<Future<?>> sendTask;
        private final AtomicReference<Consumer<ByteString>> callback;

        private RequestRecord(Long id, Consumer<ByteString> callback) {
            this.id = id;
            this.expiry = System.currentTimeMillis() + rpc.requestTimeout;
            this.sendTask = new AtomicReference<>();
            this.callback = new AtomicReference<>(callback);

            /* Make sure this record is put into the map before the reply arrives */
            RequestRecord oldRecord = requestContexts.put(id, this);
            Assertion.check(oldRecord == null, "Request record already exists for id = " + id);
        }

        private boolean checkExpire() {
            if (System.currentTimeMillis() > expiry) {
                acceptResult(null);
                return true;
            } else {
                return false;
            }
        }

        private void setSendFuture(Future<?> task) {
            Assertion.check(task != null);
            boolean success = sendTask.compareAndSet(null, task);
            Assertion.check(success, "This method should only be called once.");
        }

        private void receiveReply(ByteString reply) {
            Assertion.check(reply != null);
            acceptResult(reply);
        }

        /**
         * @param reply from the server, null to indicate a timeout
         */
        private void acceptResult(ByteString reply) {
            /* Invalidate this request record */
            requestContexts.remove(id);
            Future<?> sendTask = this.sendTask.get();
            if (sendTask != null) {
                sendTask.cancel(true);
            }

            /* Protect against duplicate replies, call the callback only once */
            Consumer<ByteString> callback = this.callback.getAndSet(null);
            if (callback != null) {
                callback.accept(reply);
            }
        }

        @Override
        protected void finalize() {
            Assertion.check(callback.get() == null);
        }
    }
}