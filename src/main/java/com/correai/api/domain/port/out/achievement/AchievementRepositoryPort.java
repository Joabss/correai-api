package com.correai.api.domain.port.out.achievement;

import com.correai.api.domain.model.achievement.BadgeCode;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Outbound port for achievement badge persistence, implemented by an infrastructure adapter.
 */
public interface AchievementRepositoryPort {

    Map<BadgeCode, Instant> findUnlockedByUserId(UUID userId);

    /**
     * Persists the badge as unlocked for the user, unless it is already unlocked.
     *
     * @return {@code true} if this call newly unlocked the badge, {@code false} if it was already unlocked.
     */
    boolean grantIfAbsent(UUID userId, BadgeCode code, Instant unlockedAt);

    /**
     * Corrects the stored unlock moment of an already-unlocked badge. Used only to fix attribution
     * that was computed before the actual qualifying date was known (e.g. evaluated before the
     * criterion-attribution logic existed); never used to move a badge's date later or to revoke it.
     */
    void correctUnlockedAt(UUID userId, BadgeCode code, Instant unlockedAt);
}
