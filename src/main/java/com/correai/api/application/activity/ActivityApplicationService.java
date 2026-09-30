package com.correai.api.application.activity;

import com.correai.api.domain.model.activity.Activity;
import com.correai.api.domain.model.pagination.PageQuery;
import com.correai.api.domain.model.pagination.PageResult;
import com.correai.api.domain.port.in.activity.ActivityCreationResult;
import com.correai.api.domain.port.in.activity.CreateActivityCommand;
import com.correai.api.domain.port.in.activity.CreateActivityUseCase;
import com.correai.api.domain.port.in.activity.ListActivitiesUseCase;
import com.correai.api.domain.port.out.activity.ActivityRepositoryPort;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class ActivityApplicationService implements CreateActivityUseCase, ListActivitiesUseCase {

    private final ActivityRepositoryPort repository;

    public ActivityApplicationService(ActivityRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public ActivityCreationResult create(CreateActivityCommand command) {
        Activity activity = Activity.create(
                command.userId(),
                command.type(),
                command.distanceKm(),
                command.durationSeconds(),
                command.trainingType(),
                command.perceivedEffort(),
                command.notes()
        );

        Activity saved = repository.save(activity);

        double totalKmMonth = totalKmCurrentMonth(command.userId());
        int streak = calculateStreak(command.userId());

        return new ActivityCreationResult(saved, totalKmMonth, streak, List.of()); // MVP: sem badges
    }

    @Override
    public PageResult<Activity> list(UUID userId, PageQuery pageQuery) {
        return repository.findByUserIdOrderByActivityDateDesc(userId, pageQuery);
    }

    private double totalKmCurrentMonth(UUID userId) {
        LocalDate start = LocalDate.now().withDayOfMonth(1);
        LocalDate end = LocalDate.now();
        return repository.findByUserIdAndActivityDateBetween(userId, start, end)
                .stream()
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

