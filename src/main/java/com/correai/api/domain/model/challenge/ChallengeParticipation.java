package com.correai.api.domain.model.challenge;

import com.correai.api.domain.model.activity.Activity;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record ChallengeParticipation(UUID challengeId, UUID userId, Instant joinedAt,
                                     ChallengeProgress progress, boolean finalized) {
    public ChallengeParticipation {
        Objects.requireNonNull(challengeId);
        Objects.requireNonNull(userId);
        Objects.requireNonNull(joinedAt);
        Objects.requireNonNull(progress);
    }

    public static ChallengeParticipation join(Challenge challenge, UUID userId, Instant now,
                                              List<Activity> activities) {
        Objects.requireNonNull(challenge);
        Objects.requireNonNull(userId);
        Objects.requireNonNull(now);
        if (challenge.publishedAt() == null || now.isBefore(challenge.publishedAt())
                || now.isBefore(challenge.startAt()) || !now.isBefore(challenge.endExclusive())) {
            throw new ChallengeConflictException("Challenge is not open for participation");
        }
        ChallengeProgress progress = ChallengeProgress.empty().refresh(challenge, userId, activities, now);
        return new ChallengeParticipation(challenge.id(), userId, now, progress, false);
    }

    public ChallengeParticipation refresh(Challenge challenge, List<Activity> activities, Instant now) {
        Objects.requireNonNull(challenge);
        Objects.requireNonNull(now);
        if (!challenge.id().equals(challengeId)) {
            throw new IllegalArgumentException("Participation belongs to another challenge");
        }
        if (now.isBefore(joinedAt)) {
            throw new IllegalArgumentException("Evaluation precedes participation");
        }
        if (finalized || progress.achievedAt() != null) return this;
        boolean hasEnded = !now.isBefore(challenge.endExclusive());
        Instant evaluationTime = hasEnded ? challenge.endExclusive().minusNanos(1) : now;
        ChallengeProgress refreshed = progress.refresh(challenge, userId, activities, evaluationTime);
        if (hasEnded && refreshed.achievedAt() != null) {
            refreshed = new ChallengeProgress(refreshed.current(), now);
        }
        return new ChallengeParticipation(challengeId, userId, joinedAt, refreshed, hasEnded);
    }
}
