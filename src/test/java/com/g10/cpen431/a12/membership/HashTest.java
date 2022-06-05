package com.g10.cpen431.a12.membership;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class HashTest {
    @Test
    void testSameKey() {
        byte[] A = ("AAAAA").getBytes(StandardCharsets.UTF_8);
        assertEquals(Hash.hash(A), Hash.hash(A));
    }

    @Test
    void testDifferentKey() {
        byte[] A = "AAAAA".getBytes(StandardCharsets.UTF_8);
        byte[] B = "BBBBB".getBytes(StandardCharsets.UTF_8);

        assertNotEquals(Hash.hash(A), Hash.hash(B));
    }
}
