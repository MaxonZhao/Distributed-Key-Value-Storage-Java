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

    public InetSocketAddress findNodeFromHash(int hash) {
        // get all possible(greater hash value) nodes given the hash value
        SortedMap<Integer, InetSocketAddress> potentialNodes = nodesTreeMap.tailMap(hash);

        // there is no nodes greater than the given hash value in hash circle
        if (potentialNodes.isEmpty()) {
            // return the first node from the hash value in the hash circle space clockwise
            return nodesTreeMap.get(nodesTreeMap.firstKey());
        }

        return potentialNodes.get(potentialNodes.firstKey());
    }

    public boolean isDataInLocalNode(long hash) {
        SortedMap<Long, InetSocketAddress> potentialNodes = nodesTreeMap.tailMap(hash);
        InetSocketAddress sa = potentialNodes.get(potentialNodes.firstKey());
        String foundNodeIpAddr = sa.getAddress().getHostAddress();
        int foundNodePort = sa.getPort();
        String localNodeIpAddr = NodeInfo.getLocalNodeInfo.getAddress().getHostAddress();
        int localNodePort = NodeInfogetLocalNodeInfo.getPort();

        return foundNodeIpAddr.equals(localNodeIpAddr) && foundNodePort == localNodePort;
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
