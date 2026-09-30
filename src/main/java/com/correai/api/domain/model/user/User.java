package com.correai.api.domain.model.user;

import java.time.Instant;
import java.util.UUID;

/**
 * Pure domain model for a User. No framework/persistence concerns here.
 */
public record User(UUID id, Instant createdAt) {

    /** Creates a brand-new anonymous user (not yet persisted). */
    public static User anonymous() {
        return new User(null, null);
    }

    /** Reconstructs a User from persistence. */
    public static User reconstruct(UUID id, Instant createdAt) {
        return new User(id, createdAt);
    }

}
