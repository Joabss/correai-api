package com.correai.api.application.common;

import com.correai.api.domain.port.out.activity.ActivityRepositoryPort;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Counts consecutive days, ending today, with at least one registered activity.
 */
public class StreakCalculator {

    private final ActivityRepositoryPort repository;
    private final Clock clock;

    public StreakCalculator(ActivityRepositoryPort repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public int currentStreak(UUID userId) {
        int streak = 0;
        LocalDate date = LocalDate.now(clock);

        while (repository.existsByUserIdAndActivityDate(userId, date)) {
            streak++;
            date = date.minusDays(1);
        }
        return streak;
    }
}
