package com.correai.api.domain.model.challenge;

import com.correai.api.domain.model.activity.Activity;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class ChallengeActivityTransition {
    private ChallengeActivityTransition() {}

    /**
     * The before snapshot must be read while holding the participant lock and both snapshots
     * must belong to the same transaction. A mutation finishing after the deadline is excluded.
     */
    public static ChallengeParticipation evaluate(Challenge challenge, ChallengeParticipation participation,
                                                  List<Activity> before, List<Activity> after,
                                                  Instant beforeTime, Instant afterTime) {
        Objects.requireNonNull(challenge);
        Objects.requireNonNull(participation);
        Objects.requireNonNull(before);
        Objects.requireNonNull(after);
        Objects.requireNonNull(beforeTime);
        Objects.requireNonNull(afterTime);
        if (afterTime.isBefore(beforeTime)) {
            throw new IllegalArgumentException("Activity mutation time cannot move backwards");
        }

        ChallengeParticipation evaluated = participation.refresh(challenge, before, beforeTime);
        if (!afterTime.isBefore(challenge.endExclusive())) {
            return evaluated.refresh(challenge, before, afterTime);
        }
        return evaluated.refresh(challenge, after, afterTime);
    }
}
