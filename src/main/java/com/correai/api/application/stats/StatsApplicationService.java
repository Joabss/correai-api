package com.correai.api.application.stats;

import com.correai.api.application.common.StreakCalculator;
import com.correai.api.domain.model.activity.Activity;
import com.correai.api.domain.port.in.stats.GetStatsSummaryUseCase;
import com.correai.api.domain.port.in.stats.StatsSummary;
import com.correai.api.domain.port.out.activity.ActivityRepositoryPort;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class StatsApplicationService implements GetStatsSummaryUseCase {

    private final ActivityRepositoryPort repository;
    private final StreakCalculator streakCalculator;
    private final Clock clock;

    public StatsApplicationService(ActivityRepositoryPort repository, StreakCalculator streakCalculator, Clock clock) {
        this.repository = repository;
        this.streakCalculator = streakCalculator;
        this.clock = clock;
    }

    @Override
    public StatsSummary getSummary(UUID userId) {
        LocalDate today = LocalDate.now(clock);

        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate monthStart = today.withDayOfMonth(1);

        List<Activity> weekActivities =
                repository.findByUserIdAndActivityDateBetween(userId, weekStart, today);

        List<Activity> monthActivities =
                repository.findByUserIdAndActivityDateBetween(userId, monthStart, today);

        double kmWeek = sumDistance(weekActivities);
        double kmMonth = sumDistance(monthActivities);
        int activitiesWeek = weekActivities.size();
        int streak = streakCalculator.currentStreak(userId);
        double longestDistance = Optional.ofNullable(repository.findLongestDistance(userId)).orElse(0.0);

        return new StatsSummary(kmWeek, kmMonth, activitiesWeek, streak, longestDistance);
    }

    private double sumDistance(List<Activity> activities) {
        return activities.stream()
                .mapToDouble(Activity::distanceKm)
                .sum();
    }
}
