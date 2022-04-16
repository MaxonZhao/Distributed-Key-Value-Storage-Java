package com.g10.cpen431.a12.membership;

import com.g10.cpen431.MembershipMessage;
import com.g10.cpen431.RpcMessage;
import com.g10.cpen431.a12.rpc.RpcService;
import com.g10.util.TimerUtil;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TimerTask;

@Log4j2
class MembershipCommunication {
    private final List<PhysicalNode> nodes;
    private final int localIndex;
    private final Random rand;

    MembershipCommunication(int localIndex, List<PhysicalNode> nodes) {
        this.nodes = nodes;
        this.localIndex = localIndex;
        this.rand = new Random();
    }

    void run() {
        registerReceive();
        startSendTask();
    }

    void registerReceive() {
        RpcService.registerHandler(RpcMessage.MEMBERSHIP_FIELD_NUMBER, rpcMessage -> {
            try {
                MembershipMessage message = rpcMessage.getMembership();
                MembershipService.notifyRemoteTimestampVectorUpdate(message.getTimeVectorList());
            } catch (Throwable e) {
                log.error(e);
            }
        });
    }

    void startSendTask() {
        TimerUtil.scheduleAtFixedRate(NodesManager.T, new TimerTask() {
            @Override
            public void run() {
                List<PhysicalNode> alive = new ArrayList<>();
                List<PhysicalNode> dead = new ArrayList<>();
                for (int i = 0; i < nodes.size(); i++) {
                    if (i != localIndex) {
                        if ((MembershipService.isAlive(i))) {
                            alive.add(nodes.get(i));
                        } else {
                            dead.add(nodes.get(i));
                        }
                    }
                }

                List<PhysicalNode> targets = new ArrayList<>();
                if (alive.size() > 0) {
                    targets.add(alive.get(rand.nextInt(alive.size())));
                }
                if (dead.size() > 0) {
                    targets.add(dead.get(rand.nextInt(dead.size())));
                }

                List<Long> timeStampVector = MembershipService.getLocalTimestampVector();
                RpcMessage message = RpcMessage.newBuilder().setMembership(
                        MembershipMessage.newBuilder().addAllTimeVector(timeStampVector)
                ).build();
                for (PhysicalNode target : targets) {
                    RpcService.sendMessageViaUdp(target.getUdpAddress(), message);
                }
            }
        });
    }
}
