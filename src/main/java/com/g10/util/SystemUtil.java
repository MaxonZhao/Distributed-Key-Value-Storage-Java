package com.g10.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.management.ManagementFactory;

public class SystemUtil {
    private static final Logger logger = LogManager.getLogger(SystemUtil.class);
    private static final int pid = getCurrentProcessId();

    public static int concurrencyLevel() {
        return Runtime.getRuntime().availableProcessors(); // TODO: try different levels
    }

    public static int getProcessId() {
        return pid;
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
}
