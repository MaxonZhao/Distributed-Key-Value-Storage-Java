package com.g10.util;

import lombok.extern.log4j.Log4j2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.*;
import java.util.Random;

@Log4j2
public class SystemUtil {
    private static final int UDP_BUFFER_SIZE = 64 * 1024 * 1024;
    private static final Random RAND = new Random();
    private static final int pid = getCurrentProcessId();
    private static final InetAddress publicAddress = getLocalPublicAddress();

    public static int concurrencyLevel() {
        return 2;
    }

    public static int getProcessId() {
        return pid;
    }

    public static InetAddress getPublicAddress() {
        return publicAddress;
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

    private static InetAddress getLocalPublicAddress() {
        try {
            InputStream checkIp = new URL("https://checkip.amazonaws.com").openStream();
            String ip = new BufferedReader(new InputStreamReader(checkIp)).readLine();
            return InetAddress.getByName(ip);
        } catch (IOException e) {
            log.error("Cannot resolve public address" + e);
            try {
                return InetAddress.getByAddress(new byte[]{127, 0, 0, 1});
            } catch (UnknownHostException ex) {
                ex.printStackTrace();
                log.fatal(ex);
                System.exit(1);
                return null;
            }
        }
    }

    public static long generateTimestampVersion() {
        return System.currentTimeMillis();
    }
}
