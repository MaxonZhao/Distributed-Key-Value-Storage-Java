package com.g10.util;

import java.util.Timer;
import java.util.TimerTask;

public class TimerUtil {
    private static final Timer shared = new Timer();

    public static void scheduleAtFixedRate(long period, TimerTask task) {
        shared.scheduleAtFixedRate(task, 0, period);
    }
}
