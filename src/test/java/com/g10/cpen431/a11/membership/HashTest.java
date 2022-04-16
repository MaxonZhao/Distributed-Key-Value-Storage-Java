package com.g10.cpen431.a11.membership;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HashTest {
    @Test
    void check_hash_with_same_key() {
        byte[] A = ("AAAAA").getBytes(StandardCharsets.UTF_8);
        System.out.println(Hash.hash(A));
        System.out.println(Hash.hash(A));
        boolean answer = Hash.hash(A) == Hash.hash(A);
        System.out.println(answer);
        assertTrue(answer);
    }

    @Test
    void check_hash_with_different_key() {
        byte[] A = "AAAAA".getBytes(StandardCharsets.UTF_8);
        byte[] B = "BBBBB".getBytes(StandardCharsets.UTF_8);

        assertNotEquals(Hash.hash(A), Hash.hash(B));
    }
}
