package com.g10.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.List;

public class NodeInfo {
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

    public static ServerList parseNodeInfo() throws IOException {
        File file = new File("C:\\Users\\robin\\Documents\\CPEN 431\\Group G10 Assignments\\cpen431_2022_project_g10\\src\\main\\java\\com\\g10\\util\\serverList.yml");
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory()); // jackson databind
        mapper.findAndRegisterModules();
        return mapper.readValue(file, ServerList.class);
    }

    public static List<InetSocketAddress> generateNodesList() {
        List<InetSocketAddress> nodes = null;
        NodeInfo.ServerList serverList = null;

        try {
            serverList = NodeInfo.parseNodeInfo();
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (NodeInfo.ServerInfo serverInfo: serverList.getServerInfo()) {
            InetSocketAddress node = new InetSocketAddress(serverInfo.getIP(), serverInfo.getPort());
            nodes.add(node);
        }
        return nodes;
    }
}