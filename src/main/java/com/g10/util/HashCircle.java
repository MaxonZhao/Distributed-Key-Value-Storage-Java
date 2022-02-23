package com.g10.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.g10.util.NodeInfo.generateNodesList;


public class HashCircle {
    // expect to get a list of servers with SocketAddress
    private static HashCircle instance;
    private SortedMap<Long, InetSocketAddress> nodesTreeMap;
    private List<InetSocketAddress> nodes;

    private HashCircle(String serverListPath) throws IOException {
        nodesTreeMap = new TreeMap<>();
        generateNodesList(serverListPath);

        for (InetSocketAddress node : this.nodes) {
            long hash = Hash.hash(node.getAddress().getAddress());
            nodesTreeMap.put(hash, node);
        }
    }

    public static HashCircle getInstance(String serverListPath) throws IOException {
        if (instance == null) {
            synchronized (HashCircle.class) {
                if (instance == null) {
                    instance = new HashCircle(serverListPath);
                }
            }
        }
        return instance;
    }

    private void generateNodesList(String serverListPath) throws IOException {
        NodeInfo.ServerList nodes = NodeInfo.parseNodeInfo(serverListPath);

        for (NodeInfo.ServerInfo si : nodes.getServerInfo()) {
            InetSocketAddress node = new InetSocketAddress(si.getIP(), si.getPort());
            this.nodes.add(node);
        }
    }

    // return: null if the requested data is in local node
    // return: the socket of the node that contains the requested data as InetSocketAddress
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
}
