package com.g10.cpen431.a12.coordinator;

import com.g10.cpen431.a12.NodeInfo;
import com.g10.util.MemoryManager;
import com.g10.util.SystemUtil;
import lombok.extern.log4j.Log4j2;

import java.net.SocketException;

/**
 * <p>
 * The Coordinator Service is the front end to accept, interpret, and reply to client requests.
 * If a request needs to be routed to another node, the local coordinator will forward it to the
 * coordinator at the target node.
 * </p>
 * <p>
 * The Coordinator Service is also responsible for caching the replies for at least 1 second.
 * This is to achieve the at-most-once semantic.
 * </p>
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

        /* Perform addition clean-ups when available memory is low */
        MemoryManager.subscribeMemoryStress(CoordinatorHandlers.cache::cleanUp);
    }
}
