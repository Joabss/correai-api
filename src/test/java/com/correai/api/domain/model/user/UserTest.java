package com.correai.api.domain.model.user;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void anonymous_shouldCreateUserWithoutIdentity() {
        User user = User.anonymous();

        assertNull(user.id());
        assertNull(user.name());
        assertNull(user.email());
        assertNull(user.createdAt());
        assertTrue(user.isAnonymous());
    }

    @Test
    void reconstruct_shouldRestoreFields() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.now();

        User user = User.reconstruct(id, "Ana", "ana@example.com", createdAt);

        assertEquals(id, user.id());
        assertEquals("Ana", user.name());
        assertEquals("ana@example.com", user.email());
        assertEquals(createdAt, user.createdAt());
        assertFalse(user.isAnonymous());
    }

    @Test
    void registered_shouldTrimNameAndNormalizeEmail() {
        User user = User.registered("  Ana ", "  Ana@Example.COM ");

        assertEquals("Ana", user.name());
        assertEquals("ana@example.com", user.email());
        assertNull(user.id());
    }

    @Test
    void registered_withBlankName_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> User.registered(" ", "ana@example.com"));
        assertThrows(IllegalArgumentException.class, () -> User.registered(null, "ana@example.com"));
    }

    @Test
    void registered_withInvalidEmail_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> User.registered("Ana", null));
        assertThrows(IllegalArgumentException.class, () -> User.registered("Ana", " "));
        assertThrows(IllegalArgumentException.class, () -> User.registered("Ana", "no-at-sign"));
        assertThrows(IllegalArgumentException.class, () -> User.registered("Ana", "@example.com"));
    }
}
