package com.g10.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.*;

public class HashCircle {
    private static final Logger logger = LogManager.getLogger(HashCircle.class);
    public static final float T = 2.0F;
    private static final float M = 1.0F;
    // expect to get a list of servers with SocketAddress
    private static HashCircle instance;
    private final TreeMap<Long, InetSocketAddress> nodesTreeMap;
    private ArrayList<Long> local_timestamp_vector;
    private ArrayList<Boolean> nodesStatus;
    private HashMap<InetSocketAddress, Integer> nodesMap;
    private final long localHash;

    private HashCircle() {
        nodesTreeMap = new TreeMap<>();
        List<InetSocketAddress> nodes = NodeInfo.getServerList();

        nodesMap = new HashMap<>();
        initializeLocalTimeStampVector(nodes.size());
        initializeNodeStatus(nodes.size());

        int i = 0;
        for (InetSocketAddress node : nodes) {
            nodesMap.put(node, i);
            long hash = Hash.hash(node.toString().getBytes());
            if (nodesTreeMap.put(hash, node) != null) {
                logger.fatal("Hash conflict! hash: {}", hash);
            }
            i++;
        }
        logger.info("nodesTreeMap size: {}, content: {}", nodesTreeMap.size(), nodesTreeMap);

        localHash = Hash.hash(NodeInfo.getLocalNodeInfo().toString().getBytes());
        logger.info("Local hash: {}", localHash);

        logRingAnalysis();

        if (logger.isInfoEnabled()) {
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    for (int i = 0; i < nodesMap.size(); ++i) {
                        if (nodesStatus.get(i)) logger.info("node # {} is alive", i);
                        else logger.info("node # {} is down", i);
                    }
                }
            }, 0, 5 * 1000);
        }
    }

    private void initializeNodeStatus(int n) {
        nodesStatus = new ArrayList<>(n);
        for (int i = 0; i < n; ++i) {
            nodesStatus.add(true);
        }
    }

    private void initializeLocalTimeStampVector(int n) {
        local_timestamp_vector = new ArrayList<>(n);
        for (int i = 0; i < n; ++i) {
            local_timestamp_vector.add(System.currentTimeMillis());
        }
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


        Map.Entry<Long, InetSocketAddress> firstNode = nodesTreeMap.firstEntry();

        if (firstNode == null) {
            /* get the hash value of the first entry of on the hash circle */
            hash = Hash.hash(firstNode.getValue().toString().getBytes());
            potentialNodes = nodesTreeMap.tailMap(hash, true);
        }

        for (Map.Entry<Long, InetSocketAddress> node : potentialNodes.entrySet()) {
            updateNodesStatus();
            if (isAlive(nodesMap.get(node.getValue()))) {
                // requested data is in local node
                if (node.getKey() == localHash) {
                    return null;
                } else {
                    return node.getValue();
                }
            }
        }

        /* get potential nodes before the hashed key in 'clockwise' order */
        potentialNodes = nodesTreeMap.headMap(hash, true).descendingMap();

        for (Map.Entry<Long, InetSocketAddress> node : potentialNodes.entrySet()) {
            updateNodesStatus();
            if (isAlive(nodesMap.get(node.getValue()))) {
                // requested data is in local node
                if (node.getKey() == localHash) {
                    return null;
                } else {
                    return node.getValue();
                }
            }
        }

        return null;

//        return node.getValue();
    }

    private boolean isNodeAlive(int i) {
        int n = this.getLocalTimestampVector().size();
        return (System.currentTimeMillis() - local_timestamp_vector.get(i) < (T * (Math.log(n) + M)) * 1000);
    }

    public ArrayList<Long> getLocalTimestampVector() {
        return this.local_timestamp_vector;
    }

    public boolean isAlive(int i) {
        return nodesStatus.get(i);
    }

    private void updateNodesStatus() {
        for (int i = 0; i < nodesStatus.size(); ++i) {
            nodesStatus.set(i, isNodeAlive(i));
        }
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
