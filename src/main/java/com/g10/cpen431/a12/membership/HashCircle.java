package com.g10.cpen431.a12.membership;

import com.g10.cpen431.a12.NodeInfo;
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
     * @param ring      the ring
     * @param hash      the hash key
     * @param inclusive false to find the node strictly after the hash,
     *                  true to find the node after or equal to hash
     * @return the next node
     */
    private static VirtualNode nextNodeInRing(NavigableMap<Long, VirtualNode> ring, long hash, boolean inclusive) {
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

    private static List<VirtualNode> nextNodesInRing(NavigableMap<Long, VirtualNode> ring, long hash, int n, boolean inclusive) {
        ArrayList<VirtualNode> result = new ArrayList<>();
        NavigableMap<Long, VirtualNode> potentialNodes = ring.tailMap(hash, inclusive);

        findNodes(potentialNodes, n, result);
        if (result.size() == n) {
            return result;
        }

        potentialNodes = ring.headMap(hash, inclusive);
        findNodes(potentialNodes, n - result.size(), result);
        return result;
    }

    /**
     * helper method to find the corresponding node from the mapped hash value of the request key
     *
     * @param potentialNodes: a list of possible nodes
     * @return InetSocketAddress: the matched virtual node
     */
    private static VirtualNode findNode(Map<Long, VirtualNode> potentialNodes) {
        for (Map.Entry<Long, VirtualNode> entry : potentialNodes.entrySet()) {
            VirtualNode vn = entry.getValue();
            if (vn.isAlive()) {
                return vn;
            }
        }
        return null;
    }

    private static void findNodes(Map<Long, VirtualNode> ring, int n, List<VirtualNode> output) {
        for (Map.Entry<Long, VirtualNode> entry : ring.entrySet()) {
            if (n <= 0) {
                break;
            }
            VirtualNode vn = entry.getValue();
            if (vn.isAlive()) {
                output.add(vn);
                n--;
            }
        }
    }

    public List<VirtualNode> findSuccessors(long hash, int n) {
        return nextNodesInRing(nodesTreeMap, hash, n, false);
    }

    public List<VirtualNode> findPredecessors(long hash, int n) {
        return nextNodesInRing(nodesTreeMap.descendingMap(), hash, n, false);
    }

    public void updatePredecessorOfLocalVirtualNode() {
        VirtualNode oldPred = local.predecessor;
        VirtualNode newPred = nextNodeInRing(nodesTreeMap.descendingMap(), local.hashCode, false);
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

        VirtualNode node = nextNodeInRing(nodesTreeMap, hash, true);

        if (node == null || node.isLocal) {
            return null;
        } else {
            return node.physicalNode;
        }
    }

    public VirtualNode getLocalVirtualNode() {
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

        logger.info("Ring: {}", nodesTreeMap.values().stream().mapToInt(value -> value.physicalIndex).toArray());
        logger.info("Load of each primary node: {}", loads);

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
