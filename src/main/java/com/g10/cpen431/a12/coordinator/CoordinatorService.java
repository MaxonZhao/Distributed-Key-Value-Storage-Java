package com.g10.cpen431.a12.coordinator;

import com.g10.cpen431.a12.NodeInfo;
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
    private static ClientCommunicator communicator;

    public static void init() throws SocketException {
        communicator = new ClientCommunicator(
                NodeInfo.getLocalPhysicalNode().getServerAddress().getPort(),
                SystemUtil.concurrencyLevel()
        );
        Context.init(communicator);
        CoordinatorCommunication.registerReceiveHandler();

        MemoryManager.subscribeMemoryStress(CoordinatorHandlers.cache::cleanUp);
    }
}
