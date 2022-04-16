package com.g10.cpen431.a11.coordinator;

import com.g10.cpen431.a11.NodeInfo;
import com.g10.util.MemoryManager;
import com.g10.util.SystemUtil;
import lombok.extern.log4j.Log4j2;

import java.net.SocketException;

/**
 * Front end to accept client requests, // tag new values with timestamp and random number
 * Send commands to primary
 */
@Log4j2
public class CoordinatorService {
    public static void init() throws SocketException {
        ClientCommunicator communicator = new ClientCommunicator(NodeInfo.getLocalPhysicalNode().getServerAddress().getPort());
        Context.init(communicator);
        communicator.start(SystemUtil.concurrencyLevel());
        CoordinatorCommunication.registerReceiveHandler();

        MemoryManager.subscribeMemoryStress(CoordinatorHandlers.cache::cleanUp);
    }
}
