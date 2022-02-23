package com.g10.util;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.g10.util.NodeInfo.getLocalNodeInfo;
import static com.g10.util.NodeInfo.getServerList;

public class HashCircle {
    // expect to get a list of servers with SocketAddress
    private static HashCircle instance;
    private SortedMap<Long, InetSocketAddress> nodesTreeMap;
    private List<InetSocketAddress> nodes;

    private HashCircle() {
        nodesTreeMap = new TreeMap<>();
        nodes = getServerList();

        for (InetSocketAddress node : this.nodes) {
            long hash = Hash.hash(ByteUtil.concat(node.getAddress().getAddress(), ByteUtil.int2leb(node.getPort())));
            nodesTreeMap.put(hash, node);
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
        String localNodeIpAddr = getLocalNodeInfo().getAddress().getHostAddress();
        int localNodePort = getLocalNodeInfo().getPort();

        // requested data is in local node
        if (foundNodeIpAddr.equals(localNodeIpAddr) && foundNodePort == localNodePort) return null;
        return foundNodeAddr;
    }
}
