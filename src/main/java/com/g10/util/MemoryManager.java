package com.g10.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class MemoryManager {
    private static final Logger logger = LogManager.getLogger(MemoryManager.class);
    private static final long MIN_HEAP_FREE = (long) (2.5 * 1024 * 1024); /* 2.5 MB */

    private static final Runtime runtime = Runtime.getRuntime();
    private static final List<Runnable> memoryStressObservers = new ArrayList<>();

    public static void subscribeMemoryStress(Runnable action) {
        memoryStressObservers.add(action);
    }

    public static boolean checkMemoryStress() {
        if (runtime.freeMemory() >= MIN_HEAP_FREE) {
            return false;
        }

        memoryStressObservers.forEach(Runnable::run);
        runtime.gc(); /* GCs should run after the cache clean-up */

        long freeMemory = runtime.freeMemory();
        if (freeMemory < MIN_HEAP_FREE) {
            logger.warn("Out of space even after GC. {} B < {} B", freeMemory, MIN_HEAP_FREE);
            return true;
        }
        return false;
    }
}
