package com.correai.api.application.activity;

import com.correai.api.application.common.StreakCalculator;
import com.correai.api.domain.model.activity.Activity;
import com.correai.api.domain.model.activity.ActivityNotFoundException;
import com.correai.api.domain.port.in.activity.DeleteActivityUseCase;
import com.correai.api.domain.port.in.activity.GetActivityUseCase;
import com.correai.api.domain.model.pagination.PageQuery;
import com.correai.api.domain.model.pagination.PageResult;
import com.correai.api.domain.port.in.activity.ActivityCreationResult;
import com.correai.api.domain.port.in.activity.CreateActivityCommand;
import com.correai.api.domain.port.in.activity.CreateActivityUseCase;
import com.correai.api.domain.port.in.activity.ListActivitiesUseCase;
import com.correai.api.domain.port.in.activity.UpdateActivityUseCase;
import com.correai.api.domain.port.in.activity.UpdateActivityCommand;
import com.correai.api.domain.port.out.activity.ActivityRepositoryPort;
import com.correai.api.domain.port.in.goal.EvaluateGoalsUseCase;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class ActivityApplicationService implements CreateActivityUseCase, ListActivitiesUseCase,
        GetActivityUseCase, DeleteActivityUseCase, UpdateActivityUseCase {

    private final ActivityRepositoryPort repository;
    private final StreakCalculator streakCalculator;
    private final Clock clock;
    private final EvaluateGoalsUseCase goals;

    public ActivityApplicationService(ActivityRepositoryPort repository, StreakCalculator streakCalculator, Clock clock,
                                      EvaluateGoalsUseCase goals) {
        this.repository = repository;
        this.streakCalculator = streakCalculator;
        this.clock = clock;
        this.goals = goals;
    }

    @Override
    public ActivityCreationResult create(CreateActivityCommand command) {
        OffsetDateTime startedAt = command.startedAt() != null
                ? command.startedAt()
                : OffsetDateTime.now(clock);
        Activity activity = Activity.createWithStartedAt(
                command.userId(),
                command.type(),
                command.distanceKm(),
                command.durationSeconds(),
                command.trainingType(),
                command.perceivedEffort(),
                command.notes(),
                startedAt
        );

        Activity saved = repository.save(activity);
        List<String> newBadges = goals.evaluate(command.userId()).stream().map(UUID::toString).toList();

        double totalKmMonth = totalKmCurrentMonth(command.userId());
        int streak = streakCalculator.currentStreak(command.userId());

        return new ActivityCreationResult(saved, totalKmMonth, streak, newBadges);
    }

    @Override
    public PageResult<Activity> list(UUID userId, PageQuery pageQuery) {
        return repository.findByUserIdOrderByActivityDateDesc(userId, pageQuery);
    }

    @Override
    public Activity get(UUID userId, UUID activityId) {
        return repository.findByIdAndUserId(activityId, userId).orElseThrow(ActivityNotFoundException::new);
    }

    @Override
    public void delete(UUID userId, UUID activityId) {
        Activity activity = get(userId, activityId);
        goals.evaluate(userId);
        repository.delete(activity);
    }

    @Override
    public Activity update(UpdateActivityCommand command) {
        Activity activity = get(command.userId(), command.activityId());
        Activity updated = activity.update(command.type(), command.distanceKm(), command.durationSeconds(),
                command.trainingType(), command.perceivedEffort(), command.notes());
        goals.evaluate(command.userId());
        Activity saved = repository.save(updated);
        goals.evaluate(command.userId());
        return saved;
    }

    private double totalKmCurrentMonth(UUID userId) {
        LocalDate end = LocalDate.now(clock);
        LocalDate start = end.withDayOfMonth(1);
        return repository.findByUserIdAndActivityDateBetween(userId, start, end)
                .stream()
                .mapToDouble(Activity::distanceKm)
                .sum();
    }
}
