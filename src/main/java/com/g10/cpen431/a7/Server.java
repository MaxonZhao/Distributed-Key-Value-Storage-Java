package com.g10.cpen431.a7;

import com.g10.util.LoggerConfiguration;
import com.g10.util.MemoryManager;
import com.g10.util.NodeInfo;
import com.g10.util.SystemUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

import static com.g10.util.NodeInfo.initializeNodesList;

public class Server {
    private static Logger logger;

    public static void main(String[] args) throws IOException, InterruptedException {
        initializeLogger(args[1]);
        MemoryManager.guardMemoryLimit();

        initializeNodesList(args[0], Integer.parseInt(args[1]));

        HashtableStorage dataModel = new HashtableStorage();
        KeyValueStorageServer server = new KeyValueStorageServer(dataModel, NodeInfo.getLocalNodeInfo().getPort());

        int numberOfThreads = SystemUtil.concurrencyLevel();
        logger.info("Starting {} threads", numberOfThreads);

        Thread[] threads = new Thread[numberOfThreads];
        for (int i = 0; i < numberOfThreads; i++) {
            threads[i] = new Thread(() -> run(server));
            threads[i].start();
        }

        TimeStampSend timeStampSend = new TimeStampSend(logger, NodeInfo.getLocalNodeInfo());
        Thread timeStampSendThread = new Thread(timeStampSend::run);
        timeStampSendThread.start();

        TimeStampReceive timeStampReceive = new TimeStampReceive(logger, NodeInfo.getLocalNodeInfo());
        Thread timeStampReceiveThread = new Thread(timeStampReceive::run);
        timeStampReceiveThread.start();

        for (int i = 0; i < numberOfThreads; i++) {
            threads[i].join();
        }

        timeStampSendThread.join();
        timeStampReceiveThread.join();
    }

    private static void run(KeyValueStorageServer server) {
        try {
            server.run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void initializeLogger(String label) {
        LoggerConfiguration.initialize(String.format("logs/server-%s.log", label));
        logger = LogManager.getLogger(Server.class);
        logger.always().log("Logger initialized.");
    }
}
