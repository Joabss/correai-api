package com.correai.api.application.challenge;

import com.correai.api.domain.model.activity.Activity;
import com.correai.api.domain.model.challenge.Challenge;
import com.correai.api.domain.model.challenge.ChallengeActivityTransition;
import com.correai.api.domain.model.challenge.ChallengeParticipation;
import com.correai.api.domain.port.out.activity.ActivityRepositoryPort;
import com.correai.api.domain.port.out.challenge.ChallengeParticipationRepositoryPort;
import com.correai.api.domain.port.out.challenge.ChallengeRepositoryPort;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ChallengeActivityEvaluationService {
    private final ChallengeParticipationRepositoryPort participations;
    private final ChallengeRepositoryPort challenges;
    private final ActivityRepositoryPort activities;
    private final Clock clock;

    public ChallengeActivityEvaluationService(ChallengeParticipationRepositoryPort participations,
                                              ChallengeRepositoryPort challenges,
                                              ActivityRepositoryPort activities, Clock clock) {
        this.participations = participations;
        this.challenges = challenges;
        this.activities = activities;
        this.clock = clock;
    }
    public record Entry(ChallengeParticipation participation, Challenge challenge) {}

    public record BeforeMutation(UUID userId, Instant time, List<Entry> entries, List<Activity> activities) {}

    public BeforeMutation begin(UUID userId) {
        participations.lockUser(userId);
        var entries = participations.findPendingByUserId(userId).stream()
                .map(p -> new Entry(p, challenges.findById(p.challengeId())
                        .orElseThrow(() -> new IllegalStateException("Participation challenge is missing"))))
                .toList();
        var before = entries.isEmpty() ? List.<Activity>of() : activities.findAllByUserId(userId);
        return new BeforeMutation(userId, clock.instant(), entries, before);
    }

    public void complete(BeforeMutation before) {
        if (before.entries().isEmpty()) return;
        List<Activity> after = activities.findAllByUserId(before.userId());
        Instant afterTime = clock.instant();
        for (Entry entry : before.entries()) {
            var revised = ChallengeActivityTransition.evaluate(entry.challenge(), entry.participation(),
                    before.activities(), after, before.time(), afterTime);
            if (!revised.equals(entry.participation())) {
                participations.update(entry.participation(), revised);
            }

        }
    }

    public void refresh(UUID userId) {
        complete(begin(userId));
    }
}
