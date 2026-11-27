package com.correai.api.domain.model.achievement;

import java.time.Instant;
import java.util.Objects;

/**
 * An achievement badge as seen from a specific user's perspective: either still locked, or
 * unlocked at a given instant.
 */
public record Achievement(BadgeCode code, boolean unlocked, Instant unlockedAt) {

    public Achievement {
        Objects.requireNonNull(code);
        if (unlocked != (unlockedAt != null)) {
            throw new IllegalArgumentException("Inconsistent achievement state");
        }
    }

    public static Achievement locked(BadgeCode code) {
        return new Achievement(code, false, null);
    }

    public static Achievement unlocked(BadgeCode code, Instant unlockedAt) {
        return new Achievement(code, true, Objects.requireNonNull(unlockedAt));
    }
}
