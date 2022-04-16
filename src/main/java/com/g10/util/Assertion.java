package com.g10.util;

public class Assertion {
    public static void check(boolean condition, String errorMessage) {
        if (!condition) {
            throw new AssertionError(errorMessage);
        }
    }

    public static void check(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }

    public static <T> void checkEquals(T expected, T actual) {
        if (!actual.equals(expected)) {
            String message = String.format("expected is: %s, but actual is: %s", expected, actual);
            throw new AssertionError(message);
        }
    }

    public static void fail(String errorMessage) {
        throw new AssertionError(errorMessage);
    }
}
