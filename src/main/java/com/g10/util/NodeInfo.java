package com.g10.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class NodeInfo {
    private static List<InetSocketAddress> serverList;
    private static InetSocketAddress self;

    public static class ServerList {
        @JsonProperty
        List<ServerInfo> serverInfo;

        public List<ServerInfo> getServerInfo() {
            return serverInfo;
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
        NodeInfo.ServerList serverList;

        serverList = NodeInfo.parseNodeInfo(serverListPath);

        for (NodeInfo.ServerInfo serverInfo: serverList.getServerInfo()) {
            InetSocketAddress node = new InetSocketAddress(serverInfo.getIP(), serverInfo.getPort());
            nodes.add(node);
        }

        NodeInfo.serverList = nodes;
        NodeInfo.self = nodes.get(selfIndex);
    }

    public static List<InetSocketAddress> getServerList() {
        return serverList;
    }

    public static InetSocketAddress getLocalNodeInfo() {
        return self;
    }
}