package com.g10.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class NodeInfo {
    private static final Logger logger = LogManager.getLogger(NodeInfo.class);
    private static List<InetSocketAddress> serverList;
    private static List<InetSocketAddress> epidemicProtocolList;
    private static InetSocketAddress self;
    private static int selfIndex;

    public static class ServerList {
        @JsonProperty
        List<ServerInfo> serverInfo;
        @JsonProperty
        List<ServerInfo> epidemicProtocolInfo;

        public List<ServerInfo> getServerInfo() {
            return serverInfo;
        }

        public List<ServerInfo> getEpidemicProtocolInfo() {
            return epidemicProtocolInfo;
        }
    }

    public static class ServerInfo {
        @JsonProperty
        private String IP;
        @JsonProperty
        private int port;

        public int getPort() {
            return port;
        }

        public String getIP() {
            return IP;
        }
    }

    public static ServerList parseNodeInfo(String serverListPath) throws IOException {
        File file = new File(serverListPath);
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory()); // jackson databind
        mapper.findAndRegisterModules();
        return mapper.readValue(file, ServerList.class);
    }

    public static void initializeNodesList(String serverListPath, int selfIndex) throws IOException {
        List<InetSocketAddress> nodes = new ArrayList<>();
        List<InetSocketAddress> epNodes = new ArrayList<>();
        NodeInfo.ServerList serverList;

        serverList = NodeInfo.parseNodeInfo(serverListPath);

        for (NodeInfo.ServerInfo serverInfo : serverList.getServerInfo()) {
            InetSocketAddress node = new InetSocketAddress(serverInfo.getIP(), serverInfo.getPort());
            nodes.add(node);
        }

        for (NodeInfo.ServerInfo epInfo : serverList.getEpidemicProtocolInfo()) {
            InetSocketAddress node = new InetSocketAddress(epInfo.getIP(), epInfo.getPort());
            epNodes.add(node);
        }

        NodeInfo.serverList = nodes;
        NodeInfo.selfIndex = selfIndex;
        NodeInfo.self = nodes.get(selfIndex);

        NodeInfo.epidemicProtocolList = epNodes;
        logger.info("Initial server list: count = {}, list: {}", NodeInfo.serverList.size(), NodeInfo.serverList);
        logger.info("Current index: {}, socket address: {}", selfIndex, NodeInfo.self);
    }

    public static List<InetSocketAddress> getServerList() {
        return serverList;
    }

    public static List<InetSocketAddress> getEpidemicProtocolList() {
        return epidemicProtocolList;
    }

    public static InetSocketAddress getLocalNodeInfo() {
        return self;
    }

    public static int getSelfIndex() {
        return selfIndex;
    }
}