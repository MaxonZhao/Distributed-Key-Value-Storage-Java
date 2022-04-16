package com.g10.cpen431.a11.membership;

import com.g10.cpen431.a11.NodeInfo;
import com.g10.util.Assertion;
import com.g10.util.TimerUtil;
import com.g10.util.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.function.Consumer;

public class MembershipService {
    private static final Logger logger = LogManager.getLogger(MembershipService.class);

    private static final ArrayList<Consumer<List<Long>>> remoteTimestampSubscribers = new ArrayList<>();
    private static final ArrayList<Consumer<List<Triple<Integer, Long, Long>>>> nodeJoinSubscribers = new ArrayList<>();
    private static final ArrayList<Consumer<ReplicaGroup>> successorListSubscribers = new ArrayList<>();
    private static final ArrayList<Consumer<VirtualNode>> predecessorSubscribers = new ArrayList<>();

    private static NodesManager nodesManager;
    private static HashCircle hashCircle;
    private static ReplicaGroup primaryReplicaGroup;
    private static MembershipCommunication communication;

    public static void init() {
        nodesManager = new NodesManager(NodeInfo.getNumberOfPhysicalNodes(), NodeInfo.getLocalIndex());
        hashCircle = HashCircle.getInstance();
        communication = new MembershipCommunication(NodeInfo.getLocalIndex(), NodeInfo.getRpcAddressList());
        primaryReplicaGroup = new ReplicaGroup(getLocalVNode());
        communication.run();

        TimerUtil.scheduleAtFixedRate(2000, new TimerTask() {
            @Override
            public void run() {
                ReplicaGroup newReplicaGroup = new ReplicaGroup(getLocalVNode());

                if (!primaryReplicaGroup.equals(newReplicaGroup)) {
                    logger.info("primaryReplicaGroup updated! old {}, new {}",
                            primaryReplicaGroup, newReplicaGroup);
                    notifySuccessorListUpdate(newReplicaGroup);
                }
                primaryReplicaGroup = newReplicaGroup;

                hashCircle.updatePredecessorOfLocalVirtualNode();
            }
        });
    }

    public static ReplicaGroup getPrimaryReplicaGroup() {
        return primaryReplicaGroup;
    }

    public static VirtualNode getLocalVNode() {
        return hashCircle.getLocalVNode();
    }

    public static VirtualNode getPredecessor() {
        ArrayList<VirtualNode> predecessors = hashCircle.findPredecessors(hashCircle.getLocalVNode().hashCode,
                ReplicaGroup.N);
        return predecessors.get(0);
    }

    public static synchronized void subscribePredecessorUpdate(Consumer<VirtualNode> eventHandler) {
        predecessorSubscribers.add(eventHandler);
    }

    static void notifyPredecessorUpdate(VirtualNode predecessor) {
        logger.info("Predecessor is updated, new predecessor is #{}", predecessor.physicalIndex);
        predecessorSubscribers.forEach(c -> c.accept(predecessor));
    }

    public static synchronized void subscribeSuccessorList(Consumer<ReplicaGroup> eventHandler) {
        successorListSubscribers.add(eventHandler);
    }

    static synchronized void notifySuccessorListUpdate(ReplicaGroup primaryReplicaGroup) {
        successorListSubscribers.forEach(c -> c.accept(primaryReplicaGroup));
    }

    public static int getNumOfAliveNodes() {
        return nodesManager.getNumOfAliveNodes();
    }

    public static PhysicalNode findPrimaryForKey(byte[] key) {
        return hashCircle.findPrimaryForKey(key);
    }

    public static synchronized void subscribeNodeJoin(Consumer<List<Triple<Integer, Long, Long>>> eventHandler) {
        nodeJoinSubscribers.add(eventHandler);
    }

    static synchronized void subscribeRemoteTimestampVector(Consumer<List<Long>> eventHandler) {
        remoteTimestampSubscribers.add(eventHandler);
    }

    static void notifyRemoteTimestampVectorUpdate(List<Long> remoteVector) {
        remoteTimestampSubscribers.forEach(c -> c.accept(remoteVector));
    }

    static void notifyNodeJoin(List<Triple<Integer, Long, Long>> nodeJoinList) {
        logger.info(() -> {
            StringBuilder sb = new StringBuilder("Rejoin detected: ");
            nodeJoinList.forEach(t -> sb.append("(# ")
                    .append(t.first).append(", ")
                    .append(t.second).append(", ")
                    .append(t.third).append(") "));
            return sb.toString();
        });

        nodeJoinSubscribers.forEach(c -> c.accept(nodeJoinList));
    }

    static List<Long> getLocalTimestampVector() {
        return nodesManager.getLocalTimestampVector();
    }

    static boolean isAlive(int physicalIndex) {
        return nodesManager.isAlive(physicalIndex);
    }
}
