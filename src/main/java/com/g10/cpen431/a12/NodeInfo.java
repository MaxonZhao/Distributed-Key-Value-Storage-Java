package com.g10.cpen431.a12;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.g10.cpen431.a12.membership.PhysicalNode;
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
    private static List<PhysicalNode> physicalNodes;
    private static PhysicalNode localPhysicalNode;
    private static int localIndex;

    private static ServerList parseNodeInfo(String serverListPath) throws IOException {
        File file = new File(serverListPath);
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.findAndRegisterModules();
        return mapper.readValue(file, ServerList.class);
    }

    public static void init(String serverListPath, int localIndex) throws IOException {
        List<PhysicalNode> physicalNodes = new ArrayList<>();
        List<ServerInfo> serverInfos = NodeInfo.parseNodeInfo(serverListPath).serverInfo;

        for (int i = 0; i < serverInfos.size(); i++) {
            NodeInfo.ServerInfo serverInfo = serverInfos.get(i);
            InetSocketAddress serverAddress = new InetSocketAddress(serverInfo.ip, serverInfo.serverPort);
            InetSocketAddress udpAddress = new InetSocketAddress(serverInfo.ip, serverInfo.udpPort);
            InetSocketAddress tcpAddress = new InetSocketAddress(serverInfo.ip, serverInfo.tcpPort);
            PhysicalNode node = new PhysicalNode(serverAddress, udpAddress, tcpAddress, i == localIndex);
            physicalNodes.add(node);
            if (i == localIndex) {
                NodeInfo.localPhysicalNode = node;
            }
        }

        NodeInfo.localIndex = localIndex;
        NodeInfo.physicalNodes = Collections.unmodifiableList(physicalNodes);
        logger.info("Current index: {}, socket address: {}", localIndex, NodeInfo.localPhysicalNode.getServerAddress());
    }

    public static int getNumberOfPhysicalNodes() {
        return physicalNodes.size();
    }

    public static List<PhysicalNode> getPhysicalNodes() {
        return physicalNodes;
    }

    public static PhysicalNode getPhysicalNode(int index) {
        return physicalNodes.get(index);
    }

    public static PhysicalNode getLocalPhysicalNode() {
        return localPhysicalNode;
    }

    public static int getLocalIndex() {
        return localIndex;
    }

    private static class ServerList {
        public List<ServerInfo> serverInfo;
    }

    private static class ServerInfo {
        public String ip;
        public int serverPort;
        public int udpPort;
        public int tcpPort;
    }
}