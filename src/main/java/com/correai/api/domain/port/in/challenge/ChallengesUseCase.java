package com.correai.api.domain.port.in.challenge;

import com.correai.api.domain.model.badge.BadgeTemplate;
import com.correai.api.domain.model.challenge.Challenge;
import com.correai.api.domain.model.challenge.ChallengeParticipation;
import com.correai.api.domain.model.pagination.PageQuery;
import com.correai.api.domain.model.pagination.PageResult;

import java.time.Instant;
import java.util.UUID;

public interface ChallengesUseCase {
    record Details(Challenge challenge, ChallengeParticipation participation) {}
    record AwardedBadge(BadgeTemplate badge, Instant achievedAt) {}

    PageResult<Challenge> list(UUID userId, PageQuery page);
    Details get(UUID userId, UUID challengeId);
    Details join(UUID userId, UUID challengeId);
    PageResult<AwardedBadge> listBadges(UUID userId, PageQuery page);
}
