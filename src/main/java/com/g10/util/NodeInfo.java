package com.g10.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.*;
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

    public static ServerList parseNodeInfo(String serverListPath) throws IOException {
        File file = new File(serverListPath);
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory()); // jackson databind
        mapper.findAndRegisterModules();
        return mapper.readValue(file, ServerList.class);
    }
}