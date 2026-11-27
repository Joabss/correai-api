package com.correai.api.application.challenge;

import com.correai.api.domain.model.challenge.Challenge;
import com.correai.api.domain.model.challenge.ChallengeAudience;
import com.correai.api.domain.model.challenge.ChallengeNotFoundException;
import com.correai.api.domain.model.challenge.ChallengeParticipation;
import com.correai.api.domain.model.pagination.PageQuery;
import com.correai.api.domain.model.pagination.PageResult;
import com.correai.api.domain.port.in.badge.BadgeTemplatesUseCase;
import com.correai.api.domain.port.in.challenge.ChallengesUseCase;
import com.correai.api.domain.port.out.activity.ActivityRepositoryPort;
import com.correai.api.domain.port.out.challenge.ChallengeParticipationRepositoryPort;
import com.correai.api.domain.port.out.challenge.ChallengeRepositoryPort;
import com.correai.api.domain.port.out.friendship.FriendshipRepositoryPort;
import com.correai.api.domain.port.out.group.GroupRepositoryPort;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class ChallengesApplicationService implements ChallengesUseCase {
    private final ChallengeRepositoryPort challenges;
    private final ChallengeParticipationRepositoryPort participations;
    private final FriendshipRepositoryPort friendships;
    private final GroupRepositoryPort groups;
    private final ActivityRepositoryPort activities;
    private final ChallengeActivityEvaluationService evaluation;
    private final BadgeTemplatesUseCase badges;
    private final Clock clock;

    public ChallengesApplicationService(ChallengeRepositoryPort challenges,
            ChallengeParticipationRepositoryPort participations, FriendshipRepositoryPort friendships,
            GroupRepositoryPort groups, ActivityRepositoryPort activities, ChallengeActivityEvaluationService evaluation,
            BadgeTemplatesUseCase badges, Clock clock) {
        this.challenges = challenges;
        this.participations = participations;
        this.friendships = friendships;
        this.groups = groups;
        this.activities = activities;
        this.evaluation = evaluation;
        this.badges = badges;
        this.clock = clock;
    }

    private Challenge lockVisible(UUID userId, UUID id) {
        Challenge challenge = challenges.findById(id)
                .filter(c -> c.publishedAt() != null).orElseThrow(ChallengeNotFoundException::new);
        friendships.lockUsersForInteraction(userId, challenge.creatorId());
        if (challenge.audience() == ChallengeAudience.GROUP
                && !groups.isMemberLocked(userId, challenge.groupId())
                && !userId.equals(challenge.creatorId())) {
            throw new ChallengeNotFoundException();
        }
        return challenges.findVisible(userId, id).orElseThrow(ChallengeNotFoundException::new);
    }

    @Override
    public PageResult<Challenge> list(UUID userId, PageQuery page) {
        return challenges.listVisible(userId, page);
    }

    @Override
    public Details get(UUID userId, UUID id) {
        Challenge challenge = lockVisible(userId, id);
        evaluation.refresh(userId);
        return new Details(challenge, participations.find(id, userId).orElse(null));
    }

    @Override
    public Details join(UUID userId, UUID id) {
        Challenge challenge = lockVisible(userId, id);
        var existing = participations.find(id, userId);
        if (existing.isPresent()) {
            evaluation.refresh(userId);
            return new Details(challenge, participations.find(id, userId).orElseThrow());
        }
        var history = activities.findAllByUserId(userId);
        var participation = ChallengeParticipation.join(challenge, userId,
                clock.instant().truncatedTo(ChronoUnit.MICROS), history);
        return new Details(challenge, participations.create(participation));
    }

    @Override
    public PageResult<AwardedBadge> listBadges(UUID userId, PageQuery page) {
        evaluation.refresh(userId);
        return participations.listAwarded(userId, page).map(participation -> {
            Challenge challenge = challenges.findById(participation.challengeId())
                    .orElseThrow(() -> new IllegalStateException("Awarded challenge is missing"));
            return new AwardedBadge(badges.get(challenge.badgeTemplateId()), participation.progress().achievedAt());
        });
    }
}
