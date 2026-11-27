package com.correai.api.adapter.out.persistence.challenge;

import com.correai.api.adapter.out.persistence.user.SpringDataUserRepository;
import com.correai.api.adapter.out.persistence.user.UserEntity;
import com.correai.api.domain.model.challenge.ChallengeConflictException;
import com.correai.api.domain.model.challenge.ChallengeParticipation;
import com.correai.api.domain.model.challenge.ChallengeProgress;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChallengeParticipationRepositoryAdapterTest {
    private final SpringDataChallengeParticipationRepository participations =
            mock(SpringDataChallengeParticipationRepository.class);
    private final SpringDataChallengeRepository challenges = mock(SpringDataChallengeRepository.class);
    private final SpringDataUserRepository users = mock(SpringDataUserRepository.class);
    private final ChallengeParticipationRepositoryAdapter adapter =
            new ChallengeParticipationRepositoryAdapter(participations, challenges, users);
    private final UUID challengeId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final Instant joined = Instant.parse("2026-06-01T12:00:00Z");
    private final ChallengeParticipation initial =
            new ChallengeParticipation(challengeId, userId, joined, new ChallengeProgress(2, null), false);

    @Test
    void duplicateJoinKeepsOriginalSnapshot() {
        when(users.lockById(userId)).thenReturn(Optional.of(mock(UserEntity.class)));
        ChallengeEntity challenge = new ChallengeEntity();
        challenge.publishedAt = joined;
        when(challenges.findById(challengeId)).thenReturn(Optional.of(challenge));
        when(participations.findByChallengeIdAndUserId(challengeId, userId))
                .thenReturn(Optional.of(new ChallengeParticipationEntity(initial)));
        var repeat = new ChallengeParticipation(challengeId, userId, joined.plusSeconds(10),
                new ChallengeProgress(5, null), false);
        assertEquals(initial, adapter.create(repeat));
        verify(participations, never()).save(any());
    }

    @Test
    void refusesToCreateParticipationForDraft() {
        when(users.lockById(userId)).thenReturn(Optional.of(mock(UserEntity.class)));
        when(challenges.findById(challengeId)).thenReturn(Optional.of(new ChallengeEntity()));
        assertThrows(ChallengeConflictException.class, () -> adapter.create(initial));
        verify(participations, never()).save(any());
    }

    @Test
    void earnedProgressCannotBeOverwritten() {
        var awarded = new ChallengeParticipation(challengeId, userId, joined,
                new ChallengeProgress(10, joined.plusSeconds(1)), false);
        when(participations.lockParticipation(challengeId, userId))
                .thenReturn(Optional.of(new ChallengeParticipationEntity(awarded)));
        assertThrows(ChallengeConflictException.class, () -> adapter.update(awarded, initial));
    }

    @Test
    void rejectsUpdateWithDifferentJoinTime() {
        when(participations.lockParticipation(challengeId, userId))
                .thenReturn(Optional.of(new ChallengeParticipationEntity(initial)));
        var stale = new ChallengeParticipation(challengeId, userId, joined.plusSeconds(1),
                ChallengeProgress.empty(), false);
        assertThrows(IllegalArgumentException.class, () -> adapter.update(initial, stale));
    }

    @Test
    void finalizedUnawardedProgressCannotBeOverwritten() {
        var finished = new ChallengeParticipation(challengeId, userId, joined, new ChallengeProgress(3, null), true);
        when(participations.lockParticipation(challengeId, userId))
                .thenReturn(Optional.of(new ChallengeParticipationEntity(finished)));
        assertThrows(ChallengeConflictException.class, () -> adapter.update(finished, initial));
    }

    @Test
    void rejectsStaleProgressInsteadOfOverwriting() {
        var newer = new ChallengeParticipation(challengeId, userId, joined, new ChallengeProgress(4, null), false);
        when(participations.lockParticipation(challengeId, userId))
                .thenReturn(Optional.of(new ChallengeParticipationEntity(newer)));
        assertThrows(ChallengeConflictException.class, () -> adapter.update(initial, newer));
    }
}
