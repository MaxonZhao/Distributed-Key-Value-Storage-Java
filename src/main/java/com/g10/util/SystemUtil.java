package com.g10.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SystemUtil {
    private static final Logger logger = LogManager.getLogger(SystemUtil.class);
    private static final Runtime runtime = Runtime.getRuntime();

    private static final long MIN_HEAP_FREE = (long)(3.5 * 1024 * 1024); /* 3.5 MB */

    public static void init() {
        NotificationCenter.subscribeMemoryStress(runtime::gc);
    }

    public static boolean checkMemoryStress() {
        if (runtime.freeMemory() >= MIN_HEAP_FREE) {
            return false;
        }
        NotificationCenter.pushMemoryStressEvent();
        long freeMemory = runtime.freeMemory();
        if (freeMemory < MIN_HEAP_FREE) {
            logger.warn("Out of space even after GC. {} B < {} B", freeMemory, MIN_HEAP_FREE);
            return true;
        }
        return false;
    }

    public static int concurrencyLevel() {
        return runtime.availableProcessors(); // TODO: try different levels
    }
}
