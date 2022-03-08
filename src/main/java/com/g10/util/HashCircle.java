package com.g10.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.*;

public class HashCircle {
    private static final Logger logger = LogManager.getLogger(HashCircle.class);
    // expect to get a list of servers with SocketAddress
    private static HashCircle instance;
    private final TreeMap<Long, InetSocketAddress> nodesTreeMap;
    private final int VNODE_NUM = 8;

    private final HashMap<InetSocketAddress, Integer> nodesMap;
    private final long localHash;

    private HashCircle() {
        nodesTreeMap = new TreeMap<>();

        nodesMap = new HashMap<>();

        initHashCircle();

//        ArrayList<Long> hashValues = Hash.set_node_num(nodes.size());
//        for (int i = 0; i < nodes.size(); ++i) {
//            InetSocketAddress node = nodes.get(i);
//            nodesMap.put(node, i);
//            nodesTreeMap.put(hashValues.get(i), node);
//        }

//        logger.info("nodesTreeMap size: {}, content: {}", nodesTreeMap.size(), nodesTreeMap);

//        localHash = hashValues.get(NodeInfo.getSelfIndex());
        localHash = Hash.hash(NodeInfo.getServerList().get(NodeInfo.getLocalIndex()).toString().getBytes());
        logger.info("Local hash: {}", localHash);

        logRingAnalysis();
    }

    private void initHashCircle() {
        List<InetSocketAddress> pNodes = NodeInfo.getServerList();
        NodesManager.initializeNodesManager(pNodes.size());

        int i = 0;
        for (InetSocketAddress pNode : pNodes) {
            nodesMap.put(pNode, i);
            long hash = Hash.hash(pNode.toString().getBytes());
            if (nodesTreeMap.put(hash, pNode) != null) {
                logger.fatal("Hash conflict! hash: {}", hash);
            }
            generateVNodes(pNode);
            i++;
        }
    }

    /**
     * a method to generate virtual node based on a physical node. A physical node "possesses" a list of virtual nodes
     * a physical node can be uniquely identified with its ip address : port number.  i.e: 192.168.0.0.1: 5
     * a virtual node of this physical node in this implementation is represented as: 192.168.0.0.1VNODE#1:5
     * where the number after "#" denotes which virtual node of the corresponding physical node
     * @param pNode: a physical node (real node)
     */
    private void generateVNodes(InetSocketAddress pNode) {
        for (int i = 0; i < VNODE_NUM; ++i) {
            String vNodeString = pNode.getHostName() + "VNODE#" + i;
            InetSocketAddress vNode = new InetSocketAddress(vNodeString, pNode.getPort());
            long hash = Hash.hash(vNode.toString().getBytes());
            if (nodesTreeMap.put(hash, pNode) != null) {
                logger.fatal("Hash conflict! hash: {}", hash);
            }
        }
    }

    /**
     * a method to find the physical node from a virtual node, each virtual node belongs to a physical node
     * @param node: the matched node on the ring
     * @return InetSocketAddress: the matched physical node represented in InetSocketAddress
     */
    public InetSocketAddress findPNodeFromVNode(InetSocketAddress node) {
        String nodeHostName = node.getHostName();
        // the physical node does not contain "#"
        if (!nodeHostName.contains("#")) return node;
        // convert the virtual node to physical node
        String vNodeNum = nodeHostName.split("#")[1];
        int digits = numOfDigits(Integer.parseInt(vNodeNum));
        String pNodeHostname = nodeHostName.substring(0, nodeHostName.length() - digits - 6);
        return new InetSocketAddress(pNodeHostname, node.getPort());
    }


    /**
     * a method to find the corresponding node from the key value
     * @param key: the key of the requested data
     * @return the matched physical key or null otherwise
     */
    // return: null if the requested data is in local node
    // return: the socket of the node that contains the requested data as InetSocketAddress
    public InetSocketAddress findNodeForKey(byte[] key) {
        long hash = Hash.hash(key);

        // get all possible(greater hash value) nodes given the hash value
        NavigableMap<Long, InetSocketAddress> potentialNodes = nodesTreeMap.tailMap(hash, true);
        InetSocketAddress matchedPNode = findPNode(potentialNodes);

        /* get potential nodes before the hashed key in 'clockwise' order */
        if (matchedPNode == null) {
            potentialNodes = nodesTreeMap.headMap(hash, true); // false?
            matchedPNode = findPNode(potentialNodes);
        }

        if (matchedPNode == null ) {
            return null;
        } else if (matchedPNode.equals(NodeInfo.getLocalNodeInfo())) {
            return null;
        }
        return matchedPNode;
    }

    /**
    *  helper method to find the corresponding physical node from the mapped hash value of the request key
    *  @param potentialNodes: a list of possible nodes represented in InetSocketAddress
    *  @return InetSocketAddress: the matched physical node represented as InetSocketAddress
    *  */
    private InetSocketAddress findPNode(NavigableMap<Long, InetSocketAddress> potentialNodes) {
        for (Map.Entry<Long, InetSocketAddress> node : potentialNodes.entrySet()) {
            InetSocketAddress pNode = findPNodeFromVNode(node.getValue());
            if (NodesManager.isAlive(nodesMap.get(pNode))) {
                // requested data is in local node
                long nodeHash = Hash.hash(pNode.toString().getBytes());
                if (nodeHash == localHash) {
                    return NodeInfo.getLocalNodeInfo();
                } else {
                    return pNode;
                }
            }
        }
        return null;
    }

    public static HashCircle getInstance() {
        if (instance == null) {
            synchronized (HashCircle.class) {
                if (instance == null) {
                    instance = new HashCircle();
                }
            }
        }
        return instance;
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
        logger.info("the standard deviation of all nodes on ring is {}\n", sd);
        logger.info("the standard deviation of {} nodes on ring with {} virtual nodes associated " +
                "with each of {} physical node is {}\n\n", nodesMap.size() * (VNODE_NUM + 1), VNODE_NUM, nodesMap.size(), sd);
    }

    private int numOfDigits(int i) {
        int digits = 0;
        while (i > 0) {
            i /= 10;
            digits++;
        }
        return digits;
    }
}
