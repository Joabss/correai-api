package com.correai.api.service;

import com.correai.api.domain.activity.Activity;
import com.correai.api.dto.StatsSummaryResponse;
import com.correai.api.repository.ActivityRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class StatsService {

    private final ActivityRepository repository;

    public StatsService(ActivityRepository repository) {
        this.repository = repository;
    }

    public StatsSummaryResponse getSummary(UUID userId) {

        LocalDate today = LocalDate.now();

        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate monthStart = today.withDayOfMonth(1);

        List<Activity> weekActivities =
                repository.findByUserIdAndActivityDateBetween(userId, weekStart, today);

        List<Activity> monthActivities =
                repository.findByUserIdAndActivityDateBetween(userId, monthStart, today);

        StatsSummaryResponse response = new StatsSummaryResponse();
        response.setKmWeek(sumDistance(weekActivities));
        response.setKmMonth(sumDistance(monthActivities));
        response.setActivitiesWeek(weekActivities.size());
        response.setStreak(calculateStreak(userId));
        response.setLongestDistance(
                Optional.ofNullable(repository.findLongestDistance(userId)).orElse(0.0)
        );

        return response;
    }

    private Double sumDistance(List<Activity> activities) {
        return activities.stream()
                .mapToDouble(Activity::getDistanceKm)
                .sum();
    }

    private Integer calculateStreak(UUID userId) {
        int streak = 0;
        LocalDate date = LocalDate.now();

        while (repository.existsByUserIdAndActivityDate(userId, date)) {
            streak++;
            date = date.minusDays(1);
        }
        return streak;
    }
}
