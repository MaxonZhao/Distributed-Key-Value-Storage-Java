package com.g10.cpen431.a7;

import ca.NetSysLab.ProtocolBuffers.KeyValueResponse;
import com.google.protobuf.ByteString;

import java.io.InputStream;
import java.net.SocketAddress;

public interface RequestReplyApplication {
    Reply handleRequest(InputStream request);

    class Reply {
        public final ByteString reply;
        public final boolean idempotent;
        public final SocketAddress targetNode;

        public Reply(ByteString reply, boolean idempotent) {
            this.reply = reply;
            this.idempotent = idempotent;
            this.targetNode = null;
        }

        public Reply(SocketAddress targetNode) {
            this.reply = null;
            this.idempotent = false;
            this.targetNode = targetNode;
        }

        public Reply(KeyValueResponse.KVResponse response, boolean idempotent) {
            this(response.toByteString(), idempotent);
        }

        public Reply(ErrCode errCode, boolean idempotent) {
            this(KeyValueResponse.KVResponse.newBuilder().setErrCode(errCode.val).build(), idempotent);
        }
    }
}
