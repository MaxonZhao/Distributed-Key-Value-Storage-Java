package com.g10.cpen431.a11;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.g10.cpen431.a11.membership.PhysicalNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NodeInfo {
    private static final Logger logger = LogManager.getLogger(NodeInfo.class);
    private static List<InetSocketAddress> serverList;
    private static List<InetSocketAddress> rpcAddressList;
    private static List<PhysicalNode> physicalNodes;
    private static PhysicalNode localPhysicalNode;
    private static InetSocketAddress localRpcAddress;
    private static int localIndex;

    private static ServerList parseNodeInfo(String serverListPath) throws IOException {
        File file = new File(serverListPath);
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory()); // jackson databind
        mapper.findAndRegisterModules();
        return mapper.readValue(file, ServerList.class);
    }

    public static void init(String serverListPath, int localIndex) throws IOException {
        List<InetSocketAddress> nodes = new ArrayList<>();
        List<InetSocketAddress> rpcAddresses = new ArrayList<>();
        List<PhysicalNode> physicalNodes = new ArrayList<>();
        List<ServerInfo> serverInfos = NodeInfo.parseNodeInfo(serverListPath).serverInfo;

        for (int i = 0; i < serverInfos.size(); i++) {
            NodeInfo.ServerInfo serverInfo = serverInfos.get(i);
            InetSocketAddress serverAddress = new InetSocketAddress(serverInfo.ip, serverInfo.serverPort);
            InetSocketAddress rpcAddress = new InetSocketAddress(serverInfo.ip, serverInfo.rpcPort);
            InetSocketAddress tcpAddress = new InetSocketAddress(serverInfo.ip, serverInfo.tcpPort);
            PhysicalNode node = new PhysicalNode(serverAddress, rpcAddress, tcpAddress, i == localIndex);
            nodes.add(serverAddress);
            rpcAddresses.add(rpcAddress);
            physicalNodes.add(node);
            if (i == localIndex) {
                NodeInfo.localPhysicalNode = node;
                NodeInfo.localRpcAddress = new InetSocketAddress(serverInfo.ip, serverInfo.rpcPort);
            }
        }

        NodeInfo.serverList = Collections.unmodifiableList(nodes);
        NodeInfo.localIndex = localIndex;
        NodeInfo.rpcAddressList = Collections.unmodifiableList(rpcAddresses);
        NodeInfo.physicalNodes = Collections.unmodifiableList(physicalNodes);
        logger.info("Initial server list: count = {}, list: {}", NodeInfo.serverList.size(), NodeInfo.serverList);
        logger.info("Current index: {}, socket address: {}", localIndex, NodeInfo.localPhysicalNode.getServerAddress());
    }

    public static int getNumberOfPhysicalNodes() {
        return serverList.size();
    }

    public static PhysicalNode getPhysicalNode(int index) {
        return physicalNodes.get(index);
    }

    public static List<InetSocketAddress> getRpcAddressList() {
        return rpcAddressList;
    }

    public static PhysicalNode getLocalPhysicalNode() {
        return localPhysicalNode;
    }

    public static int getLocalIndex() {
        return localIndex;
    }

    static InetSocketAddress getLocalRpcAddress() {
        return localRpcAddress;
    }

    private static class ServerList {
        public List<ServerInfo> serverInfo;
    }

    private static class ServerInfo {
        public String ip;
        public int serverPort;
        public int rpcPort;
        public int tcpPort;
    }
}