package com.g10.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

import static java.lang.Math.max;

public class NodesManager {
    public static final float T = 2.0F;
    private static final Logger logger = LogManager.getLogger(NodesManager.class);
    private static final float M = 1.0F;
    private static ArrayList<Long> localTimestampVector;
    private static double timeout;

    public static void initializeNodesManager(int n) {
        localTimestampVector = new ArrayList<>(n);
        for (int i = 0; i < n; ++i) {
            localTimestampVector.add(System.currentTimeMillis());
        }
        // Synchronization issues?? use readwritelock?
        timeout = T * (Math.log(n) + M) * 1000;
        printNodesStatusAtTimeInterval(5);
        logger.info("Timout is {}", timeout);
    }

    public static boolean isAlive(int i) {
        if (i == NodeInfo.getLocalIndex()) {
            return true;
        }
        return (System.currentTimeMillis() - localTimestampVector.get(i)) < timeout;
    }

    public static List<Long> getLocalTimestampVector() {
        localTimestampVector.set(NodeInfo.getLocalIndex(), System.currentTimeMillis());
        return Collections.unmodifiableList(localTimestampVector);
    }

    public static void mergeLocalTimestampVector(List<Long> remoteTimestampVector) {
        if (localTimestampVector.size() != remoteTimestampVector.size()) {
            logger.fatal("Size mismatch");
            logger.fatal("local {}: {}", localTimestampVector.size(), localTimestampVector);
            logger.fatal("remote {}: {}", remoteTimestampVector.size(), remoteTimestampVector);
            System.exit(1);
        }
        // TODO: change log level to trace to improve performance
//        logger.info("Remote {}", remoteTimestampVector);
//        logger.info("Current {}", localTimestampVector);
        for (int i = 0; i < localTimestampVector.size(); i++) {
            localTimestampVector.set(i, max(localTimestampVector.get(i), remoteTimestampVector.get(i)));
        }
        localTimestampVector.set(NodeInfo.getLocalIndex(), System.currentTimeMillis());
        logger.info("After merging: {}", NodesManager::getNodesStatusMessage);
    }

    public static int getNumOfAliveNodes() {
        int count = 0;
        for (int i = 0; i < localTimestampVector.size(); i++) {
            if (isAlive(i)) {
                count += 1;
            }
        }
        logger.info("Membership count: {}", count);
        return count;
    }

    private static void printNodesStatusAtTimeInterval(long seconds) {
        if (logger.isInfoEnabled()) {
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    logger.info(NodesManager::getNodesStatusMessage);
                }
            }, 0, seconds * 1000);
        }
    }

    private static String getNodesStatusMessage() {
        StringBuilder sb = new StringBuilder("These nodes are down: ");
        for (int i = 0; i < localTimestampVector.size(); ++i) {
            if (!isAlive(i)) {
                sb.append("# ").append(i).append(" (").append(localTimestampVector.get(i)).append(") ");
            }
        }
        if (sb.length() == "These nodes are down: ".length()) {
            sb.append("none");
        }
        return sb.toString();
    }
}
