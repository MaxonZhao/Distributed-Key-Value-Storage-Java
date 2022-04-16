package com.g10.cpen431.a11.rpc;

import com.g10.cpen431.a11.NodeInfo;
import com.g10.cpen431.a11.membership.PhysicalNode;
import com.g10.util.SystemUtil;
import com.luuo2000.drpc.DatagramRpc;
import com.luuo2000.drpc.DatagramRpcClient;
import com.luuo2000.drpc.DatagramRpcServer;

import java.net.SocketException;

public class RpcService {
    private static DatagramRpc rpc;

    public static void init() throws SocketException {
        PhysicalNode local = NodeInfo.getLocalPhysicalNode();
        // TODO: tune timeout
        rpc = new DatagramRpc(local.getRpcAddress().getPort(), SystemUtil.concurrencyLevel(), 10000, 1000);
    }

    public static DatagramRpcServer getServer() {
        return rpc.getServer();
    }

    public static DatagramRpcClient getClient() {
        return rpc.getClient();
    }
}
