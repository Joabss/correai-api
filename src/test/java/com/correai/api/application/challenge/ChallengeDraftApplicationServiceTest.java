package com.correai.api.application.challenge;

import com.correai.api.domain.model.admin.AdminAccessDeniedException;
import com.correai.api.domain.model.challenge.*;
import com.correai.api.domain.port.in.admin.AdminUseCase;
import com.correai.api.domain.port.in.badge.BadgeTemplatesUseCase;
import com.correai.api.domain.port.in.challenge.ChallengeDraftCommand;
import com.correai.api.domain.port.out.challenge.ChallengeRepositoryPort;
import com.correai.api.domain.port.out.admin.AdminRepositoryPort;
import com.correai.api.domain.port.out.group.GroupRepositoryPort;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChallengeDraftApplicationServiceTest {
    private final ChallengeRepositoryPort repository = mock(ChallengeRepositoryPort.class);
    private final GroupRepositoryPort groups = mock(GroupRepositoryPort.class);
    private final BadgeTemplatesUseCase badges = mock(BadgeTemplatesUseCase.class);
    private final AdminUseCase admins = mock(AdminUseCase.class);
    private final AdminRepositoryPort adminRepository = mock(AdminRepositoryPort.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneOffset.UTC);
    private final ChallengeDraftApplicationService service =
            new ChallengeDraftApplicationService(repository, groups, badges, admins, adminRepository, clock);
    private final UUID user = UUID.randomUUID();
    private final UUID badge = UUID.randomUUID();

    private ChallengeDraftCommand command(ChallengeAudience audience, UUID group, double target) {
        return new ChallengeDraftCommand("  Summer  ", "  Walk  ", audience, group,
                ChallengeMetric.DISTANCE_KM, target, LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30), "America/Sao_Paulo", badge);
    }

    @Test
    void createsDraftAndCalculatesExclusivePeriodInSelectedZone() {
        when(repository.create(any())).thenAnswer(i -> i.getArgument(0));
        Challenge result = service.create(user, command(ChallengeAudience.FRIENDS, null, 50));
        assertEquals("Summer", result.name());
        assertNull(result.publishedAt());
        assertEquals(Instant.parse("2026-06-01T03:00:00Z"), result.startAt());
        assertEquals(Instant.parse("2026-07-01T03:00:00Z"), result.endExclusive());
        verify(badges).get(badge);
    }

    @Test
    void validatesAuthorizationAndParameters() {
        UUID group = UUID.randomUUID();
        assertThrows(AdminAccessDeniedException.class,
                () -> service.create(user, command(ChallengeAudience.GLOBAL, null, 1)));
        assertThrows(ChallengeNotFoundException.class,
                () -> service.create(user, command(ChallengeAudience.GROUP, group, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> service.create(user, command(ChallengeAudience.FRIENDS, group, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> service.create(user, command(ChallengeAudience.FRIENDS, null, Double.NaN)));
        verifyNoInteractions(repository);
    }

    @Test
    void editsUsingTheStateThatWasRead() {
        when(repository.create(any())).thenAnswer(i -> i.getArgument(0));
        Challenge original = service.create(user, command(ChallengeAudience.FRIENDS, null, 50));
        when(repository.findOwn(user, original.id())).thenReturn(Optional.of(original));
        when(repository.saveDraft(any(), any())).thenAnswer(i -> i.getArgument(1));
        Challenge revised = service.edit(user, original.id(), command(ChallengeAudience.FRIENDS, null, 25));
        assertEquals(25, revised.target());
        verify(repository).saveDraft(original, revised);
    }

    @Test
    void publicationChecksCurrentAdministratorPermissionUnderLock() {
        when(repository.create(any())).thenAnswer(i -> i.getArgument(0));
        when(admins.isAdmin(user)).thenReturn(true);
        var draft = service.create(user, command(ChallengeAudience.GLOBAL, null, 50));
        when(repository.lockOwn(user, draft.id())).thenReturn(Optional.of(draft));
        assertThrows(AdminAccessDeniedException.class, () -> service.publish(user, draft.id()));
        verify(repository, never()).publish(any(), any());
        when(adminRepository.isAdminLocked(user)).thenReturn(true);
        when(repository.publish(eq(draft), any())).thenAnswer(i -> draft.publish(i.getArgument(1)));
        assertNotNull(service.publish(user, draft.id()).publishedAt());
    }

    @Test
    void publicationChecksCurrentGroupMembershipUnderLock() {
        UUID group = UUID.randomUUID();
        when(repository.create(any())).thenAnswer(i -> i.getArgument(0));
        when(groups.isMember(user, group)).thenReturn(true);
        var draft = service.create(user, command(ChallengeAudience.GROUP, group, 50));
        when(repository.lockOwn(user, draft.id())).thenReturn(Optional.of(draft));
        assertThrows(ChallengeNotFoundException.class, () -> service.publish(user, draft.id()));
        verify(repository, never()).publish(any(), any());
    }

    @Test
    void publishedChallengeCannotBeRevisedAndPeriodIsExclusive() {
        when(repository.create(any())).thenAnswer(i -> i.getArgument(0));
        Challenge draft = service.create(user, command(ChallengeAudience.FRIENDS, null, 5));
        assertThrows(ChallengeConflictException.class, () -> draft.publish(draft.endExclusive()));
        Challenge published = draft.publish(draft.endExclusive().minusNanos(1));
        assertThrows(ChallengeConflictException.class, () -> published.revise("x", "y",
                ChallengeAudience.FRIENDS, null, ChallengeMetric.ACTIVITIES, 1,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), "America/Sao_Paulo", badge));
        assertThrows(ChallengeConflictException.class, () -> published.publish(clock.instant()));
    }
}
