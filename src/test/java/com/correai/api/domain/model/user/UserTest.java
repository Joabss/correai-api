package com.correai.api.domain.model.user;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void anonymous_shouldCreateUserWithoutId() {
        User user = User.anonymous();

        assertNotNull(user);
        assertNull(user.id());
        assertNull(user.createdAt());
    }

    @Test
    void reconstruct_shouldRestoreFields() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.now();

        User user = User.reconstruct(id, createdAt);

        assertEquals(id, user.id());
        assertEquals(createdAt, user.createdAt());
    }
}

