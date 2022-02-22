package com.g10.util;

import java.util.ArrayList;
import java.util.List;

public class NotificationCenter {
    private static final List<Runnable> actions = new ArrayList<>();

    public static void subscribeMemoryStress(Runnable action) {
        actions.add(action);
    }

    public static void pushMemoryStressEvent() {
        actions.forEach(Runnable::run);
    }

    // TODO: improve
}
