package com.g10.cpen431.a11.membership;

import com.g10.cpen431.a11.NodeInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

class HashCircle {
    private static final Logger logger = LogManager.getLogger(HashCircle.class);
    private final TreeMap<Long, VirtualNode> nodesTreeMap;
    private final VirtualNode local;
    private final int VNODE_NUM = 1;
    private final int physicalNodesCount;

    HashCircle(int numberOfPhysicalNodes) {
        this.nodesTreeMap = new TreeMap<>();
        this.physicalNodesCount = numberOfPhysicalNodes;

        VirtualNode local = null;
        for (int i = 0; i < physicalNodesCount; i++) {
            List<VirtualNode> virtualNodes = VirtualNode.generateVirtualNodes(i, VNODE_NUM);
            virtualNodes.forEach(vn -> {
                if (nodesTreeMap.put(vn.hashCode, vn) != null) {
                    logger.fatal("Hash conflict! hash: {}", vn.hashCode);
                }
            });
            if (i == NodeInfo.getLocalIndex()) {
                local = virtualNodes.get(0);
            }
        }
        this.local = local;

        logRingAnalysis();
    }

    static HashCircle getInstance() {
        return InstanceHolder.instance;
    }

    /**
     * Find the next node after the hash key in the ring in 'clockwise' order
     *
     * @param ring
     * @param hash      the hash key
     * @param inclusive false to find the node strictly after the hash,
     *                  true to find the node after or equal to hash
     * @return the next node
     */
    private static VirtualNode nextInRing(NavigableMap<Long, VirtualNode> ring, long hash, boolean inclusive) {
        /* get all possible(greater hash value) nodes given the hash value */
        NavigableMap<Long, VirtualNode> potentialNodes = ring.tailMap(hash, inclusive);
        VirtualNode matchedNode = findNode(potentialNodes);

        /* get potential nodes before the hashed key in 'clockwise' order */
        if (matchedNode == null) {
            potentialNodes = ring.headMap(hash, true);
            matchedNode = findNode(potentialNodes);
        }

        return matchedNode;
    }

    /**
     * helper method to find the corresponding node from the mapped hash value of the request key
     *
     * @param potentialNodes: a list of possible nodes
     * @return InetSocketAddress: the matched virtual node
     */
    private static VirtualNode findNode(NavigableMap<Long, VirtualNode> potentialNodes) {
        for (Map.Entry<Long, VirtualNode> entry : potentialNodes.entrySet()) {
            VirtualNode vn = entry.getValue();
            if (vn.isAlive()) {
                return vn;
            }
        }
        return null;
    }

    public ArrayList<VirtualNode> findSuccessors(long hash, int N) {
        ArrayList<VirtualNode> successorList = new ArrayList<>();
        for (int i = 0; i < N; ++i) {
            VirtualNode vn = nextInRing(nodesTreeMap, hash, false);
            if (vn.isLocal) {
                return successorList;
            }
            successorList.add(vn);
            hash = vn.hashCode;
        }
        return successorList;
    }

    public ArrayList<VirtualNode> findPredecessors(long hash, int N) {
        ArrayList<VirtualNode> predecessorList = new ArrayList<>();
        int counter = 0;
        NavigableMap<Long, VirtualNode> predecessors = nodesTreeMap.headMap(hash, false).descendingMap();
        for (Map.Entry<Long, VirtualNode> entry : predecessors.entrySet()) {
            if (counter == N) break;
            VirtualNode vn = entry.getValue();
            if (vn.isAlive()) {
                predecessorList.add(vn);
                counter++;
            }
        }

        if (counter == N) return predecessorList;

        // in case there is not enough predecessors on the circle
        predecessors = nodesTreeMap.tailMap(hash, false).descendingMap();
        for (Map.Entry<Long, VirtualNode> entry : predecessors.entrySet()) {
            if (counter == N) break;
            VirtualNode vn = entry.getValue();
            if (vn.isAlive()) {
                predecessorList.add(vn);
                counter++;
            }
        }
        return predecessorList;
    }

    public void updatePredecessorOfLocalVirtualNode() {
        VirtualNode oldPred = local.predecessor;
        VirtualNode newPred = nextInRing(nodesTreeMap.descendingMap(), local.hashCode, false);
        if (oldPred != null && !oldPred.equals(newPred)) {
            MembershipService.notifyPredecessorUpdate(newPred);
        }
        local.predecessor = newPred;
    }

    /**
     * Find the corresponding node from the key value
     *
     * @param key: the key of the requested data
     * @return the matched physical key or null if the requested data is in local node
     */
    public PhysicalNode findPrimaryForKey(byte[] key) {
        long hash = Hash.hash(key);

        VirtualNode node = nextInRing(nodesTreeMap, hash, true);

        if (node == null || node.isLocal) {
            return null;
        } else {
            return node.physicalNode;
        }
    }

    public VirtualNode getLocalVNode() {
        return this.local;
    }

    private void logRingAnalysis() {
        if (!logger.isInfoEnabled() || nodesTreeMap.size() == 0) {
            return;
        }

        final double circumference = 100.0;
        final double longRange = ((double) Long.MAX_VALUE - Long.MIN_VALUE);

        Object[] hashes = nodesTreeMap.keySet().toArray();
        double[] ring = new double[hashes.length];
        double[] loads = new double[hashes.length];

        /* Calculate the location of each node */
        for (int i = 0; i < hashes.length; i++) {
            ring[i] = (Long) hashes[i] * (circumference / longRange) + circumference / 2;
        }

        /* Calculate the load of each node */
        loads[0] = (circumference - ring[ring.length - 1] + ring[0]) / circumference;
        for (int i = 1; i < hashes.length; i++) {
            loads[i] = (ring[i] - ring[i - 1]) / circumference;
        }

//        logger.info("Ring with C = {}: {}", circumference, ring);
//        System.out.printf("Ring with C = %.2f: %s\n\n", circumference, Arrays.toString(ring));
//        logger.info("Load of each node: {}", loads);
//        System.out.printf("Load of each node: %s\n\n", Arrays.toString(loads));

        float avg = 0;
        for (double load : loads) {
            avg += load;
        }
        avg /= loads.length;

        float sd_square = 0;
        for (double load : loads) {
            sd_square += Math.pow((load - avg), 2);
        }
        double sd = Math.sqrt(sd_square / loads.length);
        logger.info("the standard deviation of all nodes on ring is {}", sd);
        logger.info("the standard deviation of {} nodes on ring with {} virtual nodes associated " +
                        "with each of {} physical node is {}",
                physicalNodesCount * (VNODE_NUM + 1), VNODE_NUM, physicalNodesCount, sd);
    }

    private static final class InstanceHolder {
        private static final HashCircle instance = new HashCircle(NodeInfo.getNumberOfPhysicalNodes());
    }
}
