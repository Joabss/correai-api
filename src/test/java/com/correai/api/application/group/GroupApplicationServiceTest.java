package com.correai.api.application.group;

import com.correai.api.domain.model.group.Group;
import com.correai.api.domain.model.pagination.PageQuery;
import com.correai.api.domain.port.out.group.GroupRepositoryPort;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GroupApplicationServiceTest {
    private final GroupRepositoryPort repository = mock(GroupRepositoryPort.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.UTC);
    private final GroupApplicationService service = new GroupApplicationService(repository, clock);

    @Test
    void createsGroupWithTrimmedNameAndCreatorMembership() {
        UUID userId = UUID.randomUUID();
        when(repository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Group group = service.create(userId, "  Runners  ");
        assertEquals("Runners", group.name());
        assertEquals(userId, group.createdBy());
        assertEquals(clock.instant(), group.createdAt());
        assertTrue(group.member());
        verify(repository).create(group);
    }

    @Test
    void rejectsBlankOrOversizedNames() {
        assertThrows(IllegalArgumentException.class, () -> service.create(UUID.randomUUID(), "  "));
        assertThrows(IllegalArgumentException.class, () -> service.create(UUID.randomUUID(), "x".repeat(101)));
        assertThrows(IllegalArgumentException.class,
                () -> service.search(UUID.randomUUID(), "  ", new PageQuery(0, 20)));
        verifyNoInteractions(repository);
    }

    @Test
    void normalizesSearchAndDelegatesMembershipOperations() {
        UUID user = UUID.randomUUID();
        UUID group = UUID.randomUUID();
        PageQuery page = new PageQuery(0, 20);
        service.search(user, " runners ", page);
        service.join(user, group);
        service.leave(user, group);
        verify(repository).search(user, "runners", page);
        verify(repository).join(user, group);
        verify(repository).leave(user, group);
    }
}
