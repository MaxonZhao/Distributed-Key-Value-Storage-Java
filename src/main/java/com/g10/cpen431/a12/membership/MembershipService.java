package com.g10.cpen431.a12.membership;

import com.g10.cpen431.a12.NodeInfo;
import com.g10.util.TimerUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.function.Consumer;

public class MembershipService {
    private static final Logger logger = LogManager.getLogger(MembershipService.class);

    private static final ArrayList<Consumer<List<Long>>> remoteTimestampSubscribers = new ArrayList<>();
    private static final ArrayList<Consumer<ReplicaGroup>> replicaGroupSubscribers = new ArrayList<>();
    private static final ArrayList<Consumer<VirtualNode>> predecessorSubscribers = new ArrayList<>();

    private static NodesManager nodesManager;
    private static HashCircle hashCircle;
    private static ReplicaGroup replicaGroup;
    private static MembershipCommunication communication;

    public static void init() {
        nodesManager = new NodesManager(NodeInfo.getNumberOfPhysicalNodes(), NodeInfo.getLocalIndex());
        hashCircle = HashCircle.getInstance();
        communication = new MembershipCommunication(NodeInfo.getLocalIndex(), NodeInfo.getPhysicalNodes());
        replicaGroup = new ReplicaGroup(getLocalVNode());
        communication.run();

        TimerUtil.scheduleAtFixedRate(2000, new TimerTask() {
            @Override
            public void run() {
                ReplicaGroup newReplicaGroup = new ReplicaGroup(getLocalVNode());

                if (!replicaGroup.equals(newReplicaGroup)) {
                    logger.info("primaryReplicaGroup updated! old {}, new {}",
                            replicaGroup, newReplicaGroup);
                    notifyReplicaGroupUpdate(newReplicaGroup);
                }
                replicaGroup = newReplicaGroup;

                hashCircle.updatePredecessorOfLocalVirtualNode();
            }
        });
    }

    public static ReplicaGroup getReplicaGroup() {
        return replicaGroup;
    }

    public static VirtualNode getLocalVNode() {
        return hashCircle.getLocalVirtualNode();
    }

    public static synchronized void subscribePredecessorUpdate(Consumer<VirtualNode> eventHandler) {
        predecessorSubscribers.add(eventHandler);
    }

    static void notifyPredecessorUpdate(VirtualNode predecessor) {
        logger.info("Predecessor is updated, new predecessor is #{}", predecessor.physicalIndex);
        predecessorSubscribers.forEach(c -> c.accept(predecessor));
    }

    public static synchronized void subscribeReplicaGroupUpdate(Consumer<ReplicaGroup> eventHandler) {
        replicaGroupSubscribers.add(eventHandler);
    }

    static synchronized void notifyReplicaGroupUpdate(ReplicaGroup primaryReplicaGroup) {
        replicaGroupSubscribers.forEach(c -> c.accept(primaryReplicaGroup));
    }

    public static int getNumOfAliveNodes() {
        return nodesManager.getNumOfAliveNodes();
    }

    public static PhysicalNode findPrimaryForKey(byte[] key) {
        return hashCircle.findPrimaryForKey(key);
    }

    static synchronized void subscribeRemoteTimestampVector(Consumer<List<Long>> eventHandler) {
        remoteTimestampSubscribers.add(eventHandler);
    }

    static void notifyRemoteTimestampVectorUpdate(List<Long> remoteVector) {
        remoteTimestampSubscribers.forEach(c -> c.accept(remoteVector));
    }

    static List<Long> getLocalTimestampVector() {
        return nodesManager.getLocalTimestampVector();
    }

    static boolean isAlive(int physicalIndex) {
        return nodesManager.isAlive(physicalIndex);
    }
}
