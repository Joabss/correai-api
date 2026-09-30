package com.correai.api.application.stats;

import com.correai.api.application.common.StreakCalculator;
import com.correai.api.domain.model.activity.Activity;
import com.correai.api.domain.port.in.stats.GetStatsEvolutionUseCase;
import com.correai.api.domain.port.in.stats.GetStatsSummaryUseCase;
import com.correai.api.domain.port.in.stats.StatsSummary;
import com.correai.api.domain.port.in.stats.WeeklyEvolution;
import com.correai.api.domain.port.out.activity.ActivityRepositoryPort;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Optional;
import java.util.UUID;

public class StatsApplicationService implements GetStatsSummaryUseCase, GetStatsEvolutionUseCase {

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

    @Override
    public List<WeeklyEvolution> getEvolution(UUID userId, int weeks) {
        if (weeks < 1 || weeks > MAX_WEEKS) {
            throw new IllegalArgumentException("Weeks must be between 1 and " + MAX_WEEKS);
        }

        LocalDate today = LocalDate.now(clock);
        LocalDate currentWeekStart = today.with(DayOfWeek.MONDAY);
        LocalDate firstWeekStart = currentWeekStart.minusWeeks(weeks - 1L);

        Map<LocalDate, List<Activity>> byWeek = new TreeMap<>();
        for (int i = 0; i < weeks; i++) {
            byWeek.put(firstWeekStart.plusWeeks(i), new ArrayList<>());
        }
        repository.findByUserIdAndActivityDateBetween(userId, firstWeekStart, today)
                .forEach(activity -> {
                    List<Activity> bucket = byWeek.get(activity.activityDate().with(DayOfWeek.MONDAY));
                    if (bucket != null) {
                        bucket.add(activity);
                    }
                });

        return byWeek.entrySet().stream()
                .map(entry -> toWeeklyEvolution(entry.getKey(), entry.getValue()))
                .toList();
    }

    private WeeklyEvolution toWeeklyEvolution(LocalDate weekStart, List<Activity> activities) {
        double km = sumDistance(activities);
        long totalSeconds = activities.stream().mapToLong(Activity::durationSeconds).sum();
        Integer avgPace = km > 0 ? (int) (totalSeconds / km) : null;
        return new WeeklyEvolution(weekStart, km, activities.size(), avgPace);
    }

    private double sumDistance(List<Activity> activities) {
        return activities.stream()
                .mapToDouble(Activity::distanceKm)
                .sum();
    }
}
