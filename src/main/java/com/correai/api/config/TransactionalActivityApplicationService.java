package com.correai.api.config;

import com.correai.api.application.activity.ActivityApplicationService;
import com.correai.api.application.challenge.ChallengeActivityEvaluationService;
import com.correai.api.application.common.StreakCalculator;
import com.correai.api.domain.model.activity.Activity;
import com.correai.api.domain.port.in.activity.ActivityCreationResult;
import com.correai.api.domain.port.in.activity.CreateActivityCommand;
import com.correai.api.domain.port.in.activity.UpdateActivityCommand;
import com.correai.api.domain.port.in.achievement.AchievementsUseCase;
import com.correai.api.domain.port.in.goal.EvaluateGoalsUseCase;
import com.correai.api.domain.port.out.activity.ActivityRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

public class TransactionalActivityApplicationService extends ActivityApplicationService {
    public TransactionalActivityApplicationService(ActivityRepositoryPort repository, StreakCalculator streakCalculator,
            Clock clock, EvaluateGoalsUseCase goals, AchievementsUseCase achievements,
            ChallengeActivityEvaluationService challengeEvaluation) {
        super(repository, streakCalculator, clock, goals, achievements, challengeEvaluation);
    }

    @Override
    @Transactional
    public ActivityCreationResult create(CreateActivityCommand command) {
        return super.create(command);
    }

    @Override
    @Transactional
    public Activity update(UpdateActivityCommand command) {
        return super.update(command);
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID activityId) {
        super.delete(userId, activityId);
    }
}
