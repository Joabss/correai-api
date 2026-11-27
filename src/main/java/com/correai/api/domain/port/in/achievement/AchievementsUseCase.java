package com.correai.api.domain.port.in.achievement;

import com.correai.api.domain.model.achievement.Achievement;
import com.correai.api.domain.model.achievement.BadgeCode;

import java.util.List;
import java.util.UUID;

/**
 * Inbound port (use case): read and evaluate achievement badges for a user.
 */
public interface AchievementsUseCase {

    /** Lists the full badge catalog for a user, with unlocked state and timestamps. */
    List<Achievement> list(UUID userId);

    /**
     * Evaluates all active badge criteria for the user and persists any newly satisfied ones.
     *
     * @return the codes of badges unlocked by this evaluation (empty if none are new).
     */
    List<BadgeCode> evaluate(UUID userId);
}
