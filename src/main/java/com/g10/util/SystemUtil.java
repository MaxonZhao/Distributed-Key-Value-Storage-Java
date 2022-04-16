package com.g10.util;

import lombok.extern.log4j.Log4j2;

import java.lang.management.ManagementFactory;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

@Log4j2
public class SystemUtil {
    private static final int WATCHDOG_PERIOD = 2000; // in ms
    private static final int UDP_BUFFER_SIZE = 64 * 1024 * 1024;
    private static final Random RAND = new Random();
    private static final List<Consumer<Long>> suspendSubscribers = new ArrayList<>();
    private static final int pid = getCurrentProcessId();
    private static AtomicLong lastActive;

    public static int concurrencyLevel() {
        return 1; // Runtime.getRuntime().availableProcessors(); // TODO: try different levels
    }

    public static int getProcessId() {
        return pid;
    }

    public static void subscribeSuspended(Consumer<Long> handler) {
        suspendSubscribers.add(handler);
    }

    public static void startSuspendWatchdog() {
        lastActive = new AtomicLong(System.currentTimeMillis());
        TimerUtil.scheduleAtFixedRate(WATCHDOG_PERIOD, new TimerTask() {
            @Override
            public void run() {
                watchDogTick();
            }
        });
    }

    public static boolean watchDogTick() {
        if (lastActive == null) {
            return true;
        }
        long current = System.currentTimeMillis();
        long diff = current - lastActive.get();
        lastActive.set(current);
        if (diff > 5 * WATCHDOG_PERIOD) {
            log.warn("Process was stopped for {} ms", diff);
            suspendSubscribers.forEach(c -> c.accept(diff));
            return true;
        }
        return false;
    }

    public static DatagramSocket createDatagramSocket(int port) throws SocketException {
        DatagramSocket socket = new DatagramSocket(port);

        socket.setReceiveBufferSize(UDP_BUFFER_SIZE);
        socket.setSendBufferSize(UDP_BUFFER_SIZE);

        log.info("ReceiveBufferSize: {}, SendBufferSize: {}",
                socket.getReceiveBufferSize(), socket.getSendBufferSize());

        return socket;
    }

    /**
     * Obtain the current Process ID. This method is adapted from https://stackoverflow.com/a/43399977.
     *
     * @return the current Process ID.
     */
    private static int getCurrentProcessId() {
        String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
        return Integer.parseInt(pid);
    }

    public static long generateTimestampVersion() {
        long upper = System.currentTimeMillis() << 8;
        long lower = RAND.nextInt() & 0xFFL;
        return upper | lower;
    }
}
