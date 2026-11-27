package com.correai.api.application.admin;

import com.correai.api.domain.port.out.admin.AdminRepositoryPort;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class AdminApplicationServiceTest {
    @Test
    void delegatesAllOperationsWithInjectedClock() {
        var repository = mock(AdminRepositoryPort.class);
        var now = Instant.parse("2026-06-01T12:00:00Z");
        var service = new AdminApplicationService(repository, Clock.fixed(now, ZoneOffset.UTC));
        var actor = UUID.randomUUID();
        var target = UUID.randomUUID();
        when(repository.isAdmin(actor)).thenReturn(true);
        service.bootstrap(actor);
        service.grant(actor, target);
        service.revoke(actor, target);
        assertTrue(service.isAdmin(actor));
        verify(repository).bootstrap(actor, now);
        verify(repository).grant(actor, target, now);
        verify(repository).revoke(actor, target, now);
    }
}
