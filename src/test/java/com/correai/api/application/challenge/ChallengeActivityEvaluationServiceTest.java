package com.correai.api.application.challenge;

import com.correai.api.domain.model.activity.Activity;
import com.correai.api.domain.model.activity.ActivityType;
import com.correai.api.domain.model.challenge.*;
import com.correai.api.domain.port.out.activity.ActivityRepositoryPort;
import com.correai.api.domain.port.out.challenge.ChallengeParticipationRepositoryPort;
import com.correai.api.domain.port.out.challenge.ChallengeRepositoryPort;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChallengeActivityEvaluationServiceTest {
    private final ChallengeParticipationRepositoryPort participations = mock(ChallengeParticipationRepositoryPort.class);
    private final ChallengeRepositoryPort challenges = mock(ChallengeRepositoryPort.class);
    private final ActivityRepositoryPort activities = mock(ActivityRepositoryPort.class);
    private final Clock clock = mock(Clock.class);
    private final ChallengeActivityEvaluationService service =
            new ChallengeActivityEvaluationService(participations, challenges, activities, clock);
    private final UUID user = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-06-15T12:00:00Z");
    private final Challenge challenge = new Challenge(UUID.randomUUID(), user, "June", "June activity",
            ChallengeAudience.FRIENDS, null, ChallengeMetric.DISTANCE_KM, 10,
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), "America/Sao_Paulo",
            UUID.randomUUID(), now.minusSeconds(10), now.minusSeconds(5));

    @Test
    void locksBeforeReadingAndPersistsProgressAfterMutation() {
        var participation = ChallengeParticipation.join(challenge, user, now, List.of());
        var activity = Activity.reconstructWithStartedAt(UUID.randomUUID(), user, ActivityType.RUN,
                LocalDate.of(2026, 6, 15), null, now, 10.0, 3600, 360, null, null, null, now);
        when(participations.findPendingByUserId(user)).thenReturn(List.of(participation));
        when(challenges.findById(challenge.id())).thenReturn(Optional.of(challenge));
        when(activities.findAllByUserId(user)).thenReturn(List.of(), List.of(activity));
        when(clock.instant()).thenReturn(now, now.plusSeconds(1));
        var before = service.begin(user);
        service.complete(before);
        var order = inOrder(participations, challenges, activities);
        order.verify(participations).lockUser(user);
        order.verify(participations).findPendingByUserId(user);
        order.verify(challenges).findById(challenge.id());
        order.verify(activities, times(2)).findAllByUserId(user);
        var revised = new ChallengeParticipation(challenge.id(), user, now,
                new ChallengeProgress(10, now.plusSeconds(1)), false);
        order.verify(participations).update(participation, revised);
    }

    @Test
    void avoidsScanningActivitiesWhenNoProgressNeedsEvaluation() {
        when(clock.instant()).thenReturn(now);
        var before = service.begin(user);
        service.complete(before);
        verifyNoInteractions(activities, challenges);
        verify(participations, never()).update(any(), any());
    }

    @Test
    void failsIfParticipationReferencesMissingChallenge() {
        var participation = ChallengeParticipation.join(challenge, user, now, List.of());
        when(participations.findPendingByUserId(user)).thenReturn(List.of(participation));
        assertThrows(IllegalStateException.class, () -> service.begin(user));
    }
}
