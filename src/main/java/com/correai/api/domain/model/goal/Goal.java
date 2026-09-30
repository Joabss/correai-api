package com.correai.api.domain.model.goal;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Pure domain model for a Goal. No framework/persistence concerns here.
 */
public record Goal(
        UUID id,
        UUID userId,
        GoalType type,
        GoalPeriod period,
        double target,
        boolean active,
        Instant createdAt
) {

    public Goal {
        Objects.requireNonNull(userId);
        Objects.requireNonNull(type);
        Objects.requireNonNull(period);
        if (target <= 0) {
            throw new IllegalArgumentException("Target must be greater than zero");
        }
    }

    /** Creates a brand-new active Goal (not yet persisted). */
    public static Goal create(UUID userId, GoalType type, GoalPeriod period, double target) {
        return new Goal(null, userId, type, period, target, true, null);
    }

    /** Reconstructs a Goal from persistence. */
    public static Goal reconstruct(UUID id, UUID userId, GoalType type, GoalPeriod period,
                                   double target, boolean active, Instant createdAt) {
        return new Goal(id, userId, type, period, target, active, createdAt);
    }

    public Goal deactivate() {
        return new Goal(id, userId, type, period, target, false, createdAt);
    }

    public boolean sameKindAs(Goal other) {
        return type == other.type && period == other.period;
    }
}
