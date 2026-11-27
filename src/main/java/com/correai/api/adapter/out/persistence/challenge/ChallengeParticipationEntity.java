package com.correai.api.adapter.out.persistence.challenge;

import com.correai.api.domain.model.challenge.ChallengeParticipation;
import com.correai.api.domain.model.challenge.ChallengeProgress;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "challenge_participations", uniqueConstraints = @UniqueConstraint(
        name = "uk_challenge_participant", columnNames = {"challenge_id", "user_id"}))
class ChallengeParticipationEntity {
    @Id UUID id;
    @Column(nullable = false) UUID challengeId;
    @Column(nullable = false) UUID userId;
    @Column(nullable = false) Instant joinedAt;
    @Column(nullable = false) double currentProgress;
    Instant achievedAt;
    @Column(nullable = false) boolean finalized;

    protected ChallengeParticipationEntity() {}

    ChallengeParticipationEntity(ChallengeParticipation participation) {
        id = UUID.randomUUID();
        challengeId = participation.challengeId();
        userId = participation.userId();
        joinedAt = participation.joinedAt().truncatedTo(ChronoUnit.MICROS);
        apply(participation);
    }

    void apply(ChallengeParticipation participation) {
        currentProgress = participation.progress().current();
        achievedAt = participation.progress().achievedAt() == null ? null
                : participation.progress().achievedAt().truncatedTo(ChronoUnit.MICROS);
        finalized = participation.finalized();
    }

    ChallengeParticipation domain() {
        return new ChallengeParticipation(challengeId, userId, joinedAt,
                new ChallengeProgress(currentProgress, achievedAt), finalized);
    }
}
