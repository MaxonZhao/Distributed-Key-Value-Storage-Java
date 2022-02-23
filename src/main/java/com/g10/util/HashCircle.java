package com.g10.util;
import com.sun.org.apache.xalan.internal.lib.NodeInfo;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class HashCircle {
    // expect to get a list of servers with SocketAddress
    private static HashCircle instance;
    private SortedMap<Long, InetSocketAddress> nodesTreeMap;
    private List<InetSocketAddress> nodes;

    private HashCircle() {
        nodes = NodeInfo.getNodesList();
        nodes = new LinkedList<>();
        nodesTreeMap = new TreeMap<Long, InetSocketAddress>();

        for (InetSocketAddress node : nodes) {
            long hash = Hash.hash(node.getAddress().getAddress());
            nodesTreeMap.put(hash, node);
        }
    }

    public InetSocketAddress findNodeFromHash(byte[] key) {
        InetSocketAddress foundNodeAddr = null;
        long hash = Hash.hash(key);
        // get all possible(greater hash value) nodes given the hash value
        SortedMap<Long, InetSocketAddress> potentialNodes = nodesTreeMap.tailMap(hash);

        // there is no nodes greater than the given hash value in hash circle
        if (potentialNodes.isEmpty()) {
            // fetch first node from the hash value in the hash circle space clockwise
             foundNodeAddr = nodesTreeMap.get(nodesTreeMap.firstKey());
        } else {
            foundNodeAddr = potentialNodes.get(potentialNodes.firstKey());
        }

        String foundNodeIpAddr = foundNodeAddr.getAddress().getHostAddress();
        int foundNodePort = foundNodeAddr.getPort();
        String localNodeIpAddr = NodeInfo.getLocalNodeInfo.getAddress().getHostAddress();
        int localNodePort = NodeInfogetLocalNodeInfo.getPort();

        // requested data is in local node
        if (foundNodeIpAddr.equals(localNodeIpAddr) && foundNodePort == localNodePort) return null;
        return foundNodeAddr;
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
}
