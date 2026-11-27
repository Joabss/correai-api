package com.correai.api.config;

import com.correai.api.application.challenge.ChallengeActivityEvaluationService;
import com.correai.api.application.challenge.ChallengesApplicationService;
import com.correai.api.domain.model.pagination.PageQuery;
import com.correai.api.domain.model.pagination.PageResult;
import com.correai.api.domain.port.in.badge.BadgeTemplatesUseCase;
import com.correai.api.domain.port.out.activity.ActivityRepositoryPort;
import com.correai.api.domain.port.out.challenge.ChallengeParticipationRepositoryPort;
import com.correai.api.domain.port.out.challenge.ChallengeRepositoryPort;
import com.correai.api.domain.port.out.friendship.FriendshipRepositoryPort;
import com.correai.api.domain.port.out.group.GroupRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

public class TransactionalChallengesApplicationService extends ChallengesApplicationService {
    public TransactionalChallengesApplicationService(ChallengeRepositoryPort challenges,
            ChallengeParticipationRepositoryPort participations, FriendshipRepositoryPort friendships,
            GroupRepositoryPort groups, ActivityRepositoryPort activities, ChallengeActivityEvaluationService evaluation,
            BadgeTemplatesUseCase badges, Clock clock) {
        super(challenges, participations, friendships, groups, activities, evaluation, badges, clock);
    }

    @Override
    @Transactional
    public Details get(UUID userId, UUID challengeId) {
        return super.get(userId, challengeId);
    }

    @Override
    @Transactional
    public Details join(UUID userId, UUID challengeId) {
        return super.join(userId, challengeId);
    }

    @Override
    @Transactional
    public PageResult<AwardedBadge> listBadges(UUID userId, PageQuery page) {
        return super.listBadges(userId, page);
    }
}
