package com.correai.api.application.stats;

import com.correai.api.domain.model.activity.Activity;
import com.correai.api.domain.port.in.stats.GetStatsSummaryUseCase;
import com.correai.api.domain.port.in.stats.StatsSummary;
import com.correai.api.domain.port.out.activity.ActivityRepositoryPort;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class StatsApplicationService implements GetStatsSummaryUseCase {

    private final ActivityRepositoryPort repository;

    public StatsApplicationService(ActivityRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public StatsSummary getSummary(UUID userId) {
        LocalDate today = LocalDate.now();

        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate monthStart = today.withDayOfMonth(1);

        List<Activity> weekActivities =
                repository.findByUserIdAndActivityDateBetween(userId, weekStart, today);

        List<Activity> monthActivities =
                repository.findByUserIdAndActivityDateBetween(userId, monthStart, today);

        double kmWeek = sumDistance(weekActivities);
        double kmMonth = sumDistance(monthActivities);
        int activitiesWeek = weekActivities.size();
        int streak = calculateStreak(userId);
        double longestDistance = Optional.ofNullable(repository.findLongestDistance(userId)).orElse(0.0);

        return new StatsSummary(kmWeek, kmMonth, activitiesWeek, streak, longestDistance);
    }

    private double sumDistance(List<Activity> activities) {
        return activities.stream()
                .mapToDouble(Activity::distanceKm)
                .sum();
    }

    private int calculateStreak(UUID userId) {
        int streak = 0;
        LocalDate date = LocalDate.now();

        while (repository.existsByUserIdAndActivityDate(userId, date)) {
            streak++;
            date = date.minusDays(1);
        }
        return streak;
    }
}

