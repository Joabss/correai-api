package com.correai.api.application.goal;

import com.correai.api.domain.model.activity.Activity;
import com.correai.api.domain.model.goal.Goal;
import com.correai.api.domain.model.goal.GoalNotFoundException;
import com.correai.api.domain.model.goal.GoalHasBadgeException;
import com.correai.api.domain.model.goal.GoalType;
import com.correai.api.domain.model.media.MediaNotFoundException;
import com.correai.api.domain.port.in.goal.EvaluateGoalsUseCase;
import com.correai.api.domain.port.in.goal.GoalBadgesUseCase;
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

public class GoalApplicationService implements CreateGoalUseCase, ListGoalsUseCase, DeactivateGoalUseCase,
        EvaluateGoalsUseCase, GoalBadgesUseCase {

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
        evaluate(command.userId());
        goalRepository.findActiveByUserId(command.userId()).stream()
                .filter(goal::sameKindAs)
                .filter(existing -> !existing.hasBadge())
                .forEach(existing -> goalRepository.deactivateUnawarded(existing.deactivate()));
        return progressOf(goalRepository.save(goal), LocalDate.now(clock)).goal();
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
        goal = progressOf(goal, LocalDate.now(clock)).goal();
        if (goal.hasBadge()) throw new GoalHasBadgeException();
        if (goal.active()) {
            goalRepository.deactivateUnawarded(goal.deactivate());
        }
    }

    @Override
    public List<UUID> evaluate(UUID userId) {
        LocalDate today = LocalDate.now(clock);
        return goalRepository.findActiveByUserId(userId).stream()
                .filter(goal -> !goal.hasBadge())
                .map(goal -> progressOf(goal, today).goal())
                .filter(Goal::hasBadge)
                .map(Goal::id)
                .toList();
    }

    @Override
    public List<Goal> listBadges(UUID userId) {
        evaluate(userId);
        return goalRepository.findAwardedByUserId(userId);
    }

    @Override
    public Goal getBadge(UUID userId, UUID goalId) {
        Goal goal = goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new GoalNotFoundException(goalId));
        if (!goal.hasBadge()) throw new MediaNotFoundException();
        return goal;
    }

    private GoalProgress progressOf(Goal goal, LocalDate today) {
        if (goal.hasBadge()) return awardedProgress(goal);
        LocalDate start = goal.period().startOf(today);
        LocalDate end = goal.period().endOf(today);
        if (!goal.active()) return new GoalProgress(goal, start, end, null, null, false);
        List<Activity> activities = activityRepository.findByUserIdAndActivityDateBetween(goal.userId(), start, today);

        GoalProgress progress = switch (goal.type()) {
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
                double pace = activities.stream().mapToLong(Activity::durationSeconds).sum() / km;
                yield new GoalProgress(goal, start, end, pace, null, pace <= goal.target());
            }
        };
        if (progress.achieved()) {
            Goal awarded = goalRepository.award(goal.achieve(clock.instant(), start, progress.current()));
            if (awarded.hasBadge()) return awardedProgress(awarded);
            return new GoalProgress(awarded, start, end, progress.current(), progress.percentage(), false);
        }
        return progress;
    }

    private GoalProgress awardedProgress(Goal goal) {
        return new GoalProgress(goal, goal.achievedPeriodStart(), goal.period().endOf(goal.achievedPeriodStart()),
                goal.achievedCurrent(),
                goal.type() == GoalType.AVG_PACE
                        ? null : percentage(goal.achievedCurrent(), goal.target()), true);
    }

    private double totalKm(List<Activity> activities) {
        return activities.stream().mapToDouble(Activity::distanceKm).sum();
    }

    private double percentage(double current, double target) {
        return Math.min(MAX_PERCENTAGE, current / target * MAX_PERCENTAGE);
    }
}
