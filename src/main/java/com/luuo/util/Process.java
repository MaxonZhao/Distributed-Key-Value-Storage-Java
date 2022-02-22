package com.luuo.util;

import java.lang.management.ManagementFactory;

public class Process {
    private static final int pid = getCurrentProcessId();

    public static int getProcessId() {
        return pid;
    }

    /**
     * Obtain the current Process ID. This method is adapted from https://stackoverflow.com/a/43399977.
     * @return the current Process ID.
     */
    private static int getCurrentProcessId() {
        String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
        return Integer.parseInt(pid);
    }
}
