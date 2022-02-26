package com.g10.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class HashCircle {
    private static final Logger logger = LogManager.getLogger(HashCircle.class);
    // expect to get a list of servers with SocketAddress
    private static HashCircle instance;
    private final TreeMap<Long, InetSocketAddress> nodesTreeMap;
    private final long localHash;

    private HashCircle() {
        nodesTreeMap = new TreeMap<>();
        List<InetSocketAddress> nodes = NodeInfo.getServerList();

        for (InetSocketAddress node : nodes) {
            long hash = Hash.hash(node.toString().getBytes());
            if (nodesTreeMap.put(hash, node) != null) {
                logger.fatal("Hash conflict! hash: {}", hash);
            }
        }
        logger.info("nodesTreeMap size: {}, content: {}", nodesTreeMap.size(), nodesTreeMap);

        localHash = Hash.hash(NodeInfo.getLocalNodeInfo().toString().getBytes());
        logger.info("Local hash: {}", localHash);

        logRingAnalysis();
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

    // return: null if the requested data is in local node
    // return: the socket of the node that contains the requested data as InetSocketAddress
    public InetSocketAddress findNodeForKey(byte[] key) {
        long hash = Hash.hash(key);

        // get all possible(greater hash value) nodes given the hash value
        NavigableMap<Long, InetSocketAddress> potentialNodes = nodesTreeMap.tailMap(hash, true);

        // fetch first node from the hash value in the hash circle space clockwise
        Map.Entry<Long, InetSocketAddress> node = potentialNodes.firstEntry();
        if (node == null) {
            // there is no nodes greater than the given hash value in hash circle
            node = nodesTreeMap.firstEntry();
        }

        // requested data is in local node
        if (node.getKey() == localHash) {
            return null;
        }

        return node.getValue();
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

        logger.info("Ring with C = {}: {}", circumference, ring);
        logger.info("Load of each node: {}", loads);
    }
}
