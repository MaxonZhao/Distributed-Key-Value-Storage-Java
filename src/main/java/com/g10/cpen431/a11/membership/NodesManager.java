package com.g10.cpen431.a11.membership;

import com.g10.util.TimerUtil;
import com.g10.util.Triple;
import com.google.common.math.IntMath;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.RoundingMode;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

class NodesManager {
    public static final int T = 250;
    private static final int M = 26;
    private static final Logger logger = LogManager.getLogger(NodesManager.class);

    private final int timeout;
    private final int numberOfNodes;
    private final int localIndex;
    private final AtomicLong[] localTimestampVector;
    private final List<Long> immutableTimestampVector;

    NodesManager(int numberOfNodes, int localIndex) {
        long currentTime = System.currentTimeMillis();
        this.numberOfNodes = numberOfNodes;
        this.localIndex = localIndex;
        this.timeout = T * (IntMath.log2(numberOfNodes, RoundingMode.CEILING) + M);

        this.immutableTimestampVector = new AbstractList<Long>() {
            @Override
            public Long get(int index) {
                return localTimestampVector[index].get();
            }

            @Override
            public int size() {
                return numberOfNodes;
            }
        };

        this.localTimestampVector = new AtomicLong[numberOfNodes];
        for (int i = 0; i < numberOfNodes; ++i) {
            this.localTimestampVector[i] = new AtomicLong(currentTime);
        }

        MembershipService.subscribeRemoteTimestampVector(this::mergeTimestampVectors);

        printNodesStatusAtTimeInterval(5);
        logger.info("Timout is {}", timeout);
    }

    boolean isAlive(int i) {
        if (i == localIndex) {
            return true;
        }
        long current = System.currentTimeMillis();
        long last = localTimestampVector[i].get();
        return (current - last) < timeout;
    }

    List<Long> getLocalTimestampVector() {
        updateLocalTimestamp();
        return immutableTimestampVector;
    }

    int getNumOfAliveNodes() {
        int count = 0;
        for (int i = 0; i < numberOfNodes; i++) {
            if (isAlive(i)) {
                count += 1;
            }
        }

        logger.info("Membership count: {} out of {} is alive", count, numberOfNodes);
        if (count < numberOfNodes) {
            logger.info(NodesManager.this::getNodesStatusMessage);
        }

        return count;
    }

    private void updateLocalTimestamp() {
        localTimestampVector[localIndex].set(System.currentTimeMillis());
    }

    private void mergeTimestampVectors(List<Long> remoteTimestampVector) {
        if (localTimestampVector.length != remoteTimestampVector.size()) {
            logger.fatal("Size mismatch");
            logger.fatal("local {}: {}", localTimestampVector.length, localTimestampVector);
            logger.fatal("remote {}: {}", remoteTimestampVector.size(), remoteTimestampVector);
            System.exit(1);
        }
        updateLocalTimestamp();

        List<Triple<Integer, Long, Long>> nodeJoin = new ArrayList<>();
        for (int i = 0; i < numberOfNodes; i++) {
            boolean aliveBefore = isAlive(i);
            long localTime = localTimestampVector[i].get();
            long remoteTime = remoteTimestampVector.get(i);
            if (remoteTime > localTime) {
                localTimestampVector[i].set(remoteTime);
            }
            boolean aliveAfter = isAlive(i);
            if (!aliveBefore && aliveAfter) {
                nodeJoin.add(new Triple<>(i, localTime, remoteTime));
            }
        }
        if (nodeJoin.size() > 0) {
            MembershipService.notifyNodeJoin(nodeJoin);
        }
    }

    private void printNodesStatusAtTimeInterval(long seconds) {
        if (logger.isInfoEnabled()) {
            TimerUtil.scheduleAtFixedRate(seconds * 1000, new TimerTask() {
                @Override
                public void run() {
                    logger.info(NodesManager.this::getNodesStatusMessage);
                }
            });
        }
    }

    private String getNodesStatusMessage() {
        StringBuilder sb = new StringBuilder();
        int failCount = 0;
        for (int i = 0; i < numberOfNodes; ++i) {
            if (!isAlive(i)) {
                failCount += 1;
                sb.append("# ").append(i).append(" (").append(localTimestampVector[i].get()).append(") ");
            }
        }
        if (sb.length() == 0) {
            sb.append("none");
        }
        return String.format("These %d nodes are down: %s", failCount, sb);
    }
}
