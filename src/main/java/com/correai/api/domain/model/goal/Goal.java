package com.correai.api.domain.model.goal;

import java.time.Instant;
import java.time.LocalDate;
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
        Instant createdAt,
        Instant achievedAt,
        LocalDate achievedPeriodStart,
        Double achievedCurrent
) {

    public Goal {
        Objects.requireNonNull(userId);
        Objects.requireNonNull(type);
        Objects.requireNonNull(period);
        if (!Double.isFinite(target) || target <= 0) {
            throw new IllegalArgumentException("Target must be greater than zero");
        }
        if ((achievedAt == null) != (achievedPeriodStart == null)
                || (achievedAt == null) != (achievedCurrent == null)) {
            throw new IllegalArgumentException("Incomplete achievement");
        }
    }

    /** Creates a brand-new active Goal (not yet persisted). */
    public static Goal create(UUID userId, GoalType type, GoalPeriod period, double target) {
        return new Goal(null, userId, type, period, target, true, null, null, null, null);
    }

    /** Reconstructs a Goal from persistence. */
    public static Goal reconstruct(UUID id, UUID userId, GoalType type, GoalPeriod period,
                                   double target, boolean active, Instant createdAt) {
        return new Goal(id, userId, type, period, target, active, createdAt, null, null, null);
    }

    public Goal achieve(Instant at, LocalDate periodStart, double current) {
        if (hasBadge()) return this;
        if (!active || !Double.isFinite(current)
                || (type == GoalType.AVG_PACE ? current <= 0 || current > target : current < target)) {
            throw new IllegalArgumentException("Goal has not been achieved");
        }
        return new Goal(id, userId, type, period, target, active, createdAt,
                Objects.requireNonNull(at), Objects.requireNonNull(periodStart), current);
    }

    public boolean hasBadge() {
        return achievedAt != null;
    }

    public Goal deactivate() {
        if (hasBadge()) throw new GoalHasBadgeException();
        return new Goal(id, userId, type, period, target, false, createdAt, null, null, null);
    }

    public boolean sameKindAs(Goal other) {
        return type == other.type && period == other.period;
    }
}
