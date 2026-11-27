package com.correai.api.application.challenge;

import com.correai.api.domain.model.challenge.*;
import com.correai.api.domain.port.in.badge.BadgeTemplatesUseCase;
import com.correai.api.domain.port.out.activity.ActivityRepositoryPort;
import com.correai.api.domain.port.out.challenge.ChallengeParticipationRepositoryPort;
import com.correai.api.domain.port.out.challenge.ChallengeRepositoryPort;
import com.correai.api.domain.port.out.friendship.FriendshipRepositoryPort;
import com.correai.api.domain.port.out.group.GroupRepositoryPort;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChallengesApplicationServiceTest {
    private final ChallengeRepositoryPort challenges = mock(ChallengeRepositoryPort.class);
    private final ChallengeParticipationRepositoryPort participations = mock(ChallengeParticipationRepositoryPort.class);
    private final FriendshipRepositoryPort friendships = mock(FriendshipRepositoryPort.class);
    private final GroupRepositoryPort groups = mock(GroupRepositoryPort.class);
    private final ActivityRepositoryPort activities = mock(ActivityRepositoryPort.class);
    private final ChallengeActivityEvaluationService evaluation = mock(ChallengeActivityEvaluationService.class);
    private final BadgeTemplatesUseCase badges = mock(BadgeTemplatesUseCase.class);
    private final Instant now = Instant.parse("2026-06-15T12:00:00Z");
    private final UUID creator = UUID.randomUUID();
    private final UUID user = UUID.randomUUID();
    private final Challenge challenge = new Challenge(UUID.randomUUID(), creator, "June", "June activity",
            ChallengeAudience.FRIENDS, null, ChallengeMetric.DISTANCE_KM, 10,
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), "America/Sao_Paulo",
            UUID.randomUUID(), now.minusSeconds(10), now.minusSeconds(5));
    private final ChallengesApplicationService service = new ChallengesApplicationService(challenges, participations,
            friendships, groups, activities, evaluation, badges, Clock.fixed(now, ZoneOffset.UTC));

    @Test
    void joinsVisibleChallengeAfterLockingBothUsers() {
        when(challenges.findById(challenge.id())).thenReturn(Optional.of(challenge));
        when(challenges.findVisible(user, challenge.id())).thenReturn(Optional.of(challenge));
        when(activities.findAllByUserId(user)).thenReturn(List.of());
        when(participations.create(any())).thenAnswer(i -> i.getArgument(0));
        var result = service.join(user, challenge.id());
        assertEquals(user, result.participation().userId());
        var order = inOrder(challenges, friendships, participations, activities);
        order.verify(challenges).findById(challenge.id());
        order.verify(friendships).lockUsersForInteraction(user, creator);
        order.verify(challenges).findVisible(user, challenge.id());
        order.verify(participations).find(challenge.id(), user);
        order.verify(activities).findAllByUserId(user);
        order.verify(participations).create(result.participation());
    }

    @Test
    void refusesParticipationWhenVisibilityWasRevoked() {
        when(challenges.findById(challenge.id())).thenReturn(Optional.of(challenge));
        assertThrows(ChallengeNotFoundException.class, () -> service.join(user, challenge.id()));
        verify(friendships).lockUsersForInteraction(user, creator);
        verifyNoInteractions(activities, evaluation);
        verify(participations, never()).create(any());
    }

    @Test
    void existingJoinIsIdempotentAndRefreshesProgress() {
        var existing = ChallengeParticipation.join(challenge, user, now, List.of());
        when(challenges.findById(challenge.id())).thenReturn(Optional.of(challenge));
        when(challenges.findVisible(user, challenge.id())).thenReturn(Optional.of(challenge));
        when(participations.find(challenge.id(), user)).thenReturn(Optional.of(existing));
        assertEquals(existing, service.join(user, challenge.id()).participation());
        verify(evaluation).refresh(user);
        verify(participations, never()).create(any());
    }

    @Test
    void refusesJoiningAfterEnd() {
        Challenge ended = new Challenge(challenge.id(), creator, challenge.name(), challenge.description(),
                challenge.audience(), null, challenge.metric(), challenge.target(),
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 2), challenge.timeZone(),
                challenge.badgeTemplateId(), now.minusSeconds(10), now.minusSeconds(5));
        when(challenges.findById(ended.id())).thenReturn(Optional.of(ended));
        when(challenges.findVisible(user, ended.id())).thenReturn(Optional.of(ended));
        assertThrows(ChallengeConflictException.class, () -> service.join(user, ended.id()));
        verify(participations, never()).create(any());
    }
}
