package com.correai.api.adapter.out.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BcryptPasswordHasherAdapterTest {

    private final BcryptPasswordHasherAdapter hasher = new BcryptPasswordHasherAdapter();

    @Test
    void hash_shouldNotExposePlainTextAndShouldMatchOriginal() {
        String hash = hasher.hash("secret-123");

        assertNotEquals("secret-123", hash);
        assertTrue(hasher.matches("secret-123", hash));
        assertFalse(hasher.matches("other-password", hash));
    }

    @Test
    void hash_shouldUseRandomSalt() {
        assertNotEquals(hasher.hash("secret-123"), hasher.hash("secret-123"));
    }
}
