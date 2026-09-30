package com.correai.api.application.goal;

import com.correai.api.domain.model.activity.Activity;
import com.correai.api.domain.model.goal.Goal;
import com.correai.api.domain.model.goal.GoalNotFoundException;
import com.correai.api.domain.port.in.goal.CreateGoalCommand;
import com.correai.api.domain.port.in.goal.CreateGoalUseCase;
import com.correai.api.domain.port.in.goal.DeactivateGoalUseCase;
import com.correai.api.domain.port.in.goal.GoalProgress;
import com.correai.api.domain.port.in.goal.ListGoalsUseCase;
import com.correai.api.domain.port.out.activity.ActivityRepositoryPort;
import com.correai.api.domain.port.out.goal.GoalRepositoryPort;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class GoalApplicationService implements CreateGoalUseCase, ListGoalsUseCase, DeactivateGoalUseCase {

    private static final double MAX_PERCENTAGE = 100.0;

    private final GoalRepositoryPort goalRepository;
    private final ActivityRepositoryPort activityRepository;
    private final Clock clock;

    public GoalApplicationService(GoalRepositoryPort goalRepository, ActivityRepositoryPort activityRepository, Clock clock) {
        this.goalRepository = goalRepository;
        this.activityRepository = activityRepository;
        this.clock = clock;
    }

    @Override
    public Goal create(CreateGoalCommand command) {
        Goal goal = Goal.create(command.userId(), command.type(), command.period(), command.target());
        goalRepository.findActiveByUserId(command.userId()).stream()
                .filter(goal::sameKindAs)
                .forEach(existing -> goalRepository.save(existing.deactivate()));
        return goalRepository.save(goal);
    }

    @Override
    public List<GoalProgress> listActive(UUID userId) {
        LocalDate today = LocalDate.now(clock);
        return goalRepository.findActiveByUserId(userId).stream()
                .map(goal -> progressOf(goal, today))
                .toList();
    }

    @Override
    public void deactivate(UUID userId, UUID goalId) {
        Goal goal = goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new GoalNotFoundException(goalId));
        if (goal.active()) {
            goalRepository.save(goal.deactivate());
        }
    }

    private GoalProgress progressOf(Goal goal, LocalDate today) {
        LocalDate start = goal.period().startOf(today);
        LocalDate end = goal.period().endOf(today);
        List<Activity> activities = activityRepository.findByUserIdAndActivityDateBetween(goal.userId(), start, today);

        return switch (goal.type()) {
            case DISTANCE_KM -> {
                double km = totalKm(activities);
                yield new GoalProgress(goal, start, end, km, percentage(km, goal.target()), km >= goal.target());
            }
            case ACTIVITIES -> {
                double count = activities.size();
                yield new GoalProgress(goal, start, end, count, percentage(count, goal.target()), count >= goal.target());
            }
            case AVG_PACE -> {
                double km = totalKm(activities);
                if (km <= 0) {
                    yield new GoalProgress(goal, start, end, null, null, false);
                }
                double pace = (int) (activities.stream().mapToLong(Activity::durationSeconds).sum() / km);
                yield new GoalProgress(goal, start, end, pace, null, pace <= goal.target());
            }
        };
    }

    private double totalKm(List<Activity> activities) {
        return activities.stream().mapToDouble(Activity::distanceKm).sum();
    }

    private double percentage(double current, double target) {
        return Math.min(MAX_PERCENTAGE, current / target * MAX_PERCENTAGE);
    }
}
