package com.g10.cpen431.a11.membership;

import com.g10.cpen431.MembershipMessage;
import com.g10.cpen431.RpcPayloadType;
import com.g10.cpen431.a11.rpc.RpcService;
import com.g10.util.TimerUtil;
import com.google.protobuf.ByteString;
import com.luuo2000.drpc.DatagramRpcClient;
import lombok.extern.log4j.Log4j2;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TimerTask;

@Log4j2
class MembershipCommunication {
    private final List<InetSocketAddress> rpcList;
    private final int localIndex;
    private final Random rand;

    MembershipCommunication(int localIndex, List<InetSocketAddress> rpcList) {
        this.rpcList = rpcList;
        this.localIndex = localIndex;
        this.rand = new Random();
    }

    void run() {
        registerReceive();
        startSendTask();
    }

    void registerReceive() {
        RpcService.getServer().registerHandler(RpcPayloadType.MEMBERSHIP_VALUE, requestContext -> {
            try {
                MembershipMessage message = MembershipMessage.parseFrom(requestContext.getPayload());
                MembershipService.notifyRemoteTimestampVectorUpdate(message.getTimeVectorList());
            } catch (Throwable e) {
                log.error(e);
            }
        });
    }

    void startSendTask() {
        DatagramRpcClient rpcClient = RpcService.getClient();
        TimerUtil.scheduleAtFixedRate(NodesManager.T, new TimerTask() {
            @Override
            public void run() {
                List<InetSocketAddress> alive = new ArrayList<>();
                List<InetSocketAddress> dead = new ArrayList<>();
                for (int i = 0; i < rpcList.size(); i++) {
                    if (i != localIndex) {
                        if ((MembershipService.isAlive(i))) {
                            alive.add(rpcList.get(i));
                        } else {
                            dead.add(rpcList.get(i));
                        }
                    }
                }

                List<InetSocketAddress> targets = new ArrayList<>();
                if (alive.size() > 0) {
                    targets.add(alive.get(rand.nextInt(alive.size())));
                }
                if (dead.size() > 0) {
                    targets.add(dead.get(rand.nextInt(dead.size())));
                }

                List<Long> timeStampVector = MembershipService.getLocalTimestampVector();
                ByteString message = MembershipMessage.newBuilder()
                        .addAllTimeVector(timeStampVector)
                        .build().toByteString();
                for (InetSocketAddress address : targets) {
                    rpcClient.sendMessageUnreliably(address, RpcPayloadType.MEMBERSHIP_VALUE, message);
                }
            }
        });
    }
}
