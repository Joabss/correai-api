package com.correai.api.application.badge;

import com.correai.api.domain.model.admin.AdminAccessDeniedException;
import com.correai.api.domain.model.badge.BadgeIcon;
import com.correai.api.domain.model.badge.BadgeTemplate;
import com.correai.api.domain.model.badge.BadgeTemplateNotFoundException;
import com.correai.api.domain.port.in.admin.AdminUseCase;
import com.correai.api.domain.port.out.badge.BadgeTemplateRepositoryPort;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BadgeTemplateApplicationServiceTest {
    private final BadgeTemplateRepositoryPort repository = mock(BadgeTemplateRepositoryPort.class);
    private final AdminUseCase admins = mock(AdminUseCase.class);
    private final Instant now = Instant.parse("2026-06-01T12:00:00Z");
    private final BadgeTemplateApplicationService service =
            new BadgeTemplateApplicationService(repository, admins, Clock.fixed(now, ZoneOffset.UTC));

    @Test
    void createsNormalizedImmutableTemplateForAdmin() {
        UUID actor = UUID.randomUUID();
        when(admins.isAdmin(actor)).thenReturn(true);
        when(repository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));
        BadgeTemplate template = service.create(actor, " Badge ", " Description ", BadgeIcon.FOOTPRINT, "#00aabb");
        assertEquals("Badge", template.name());
        assertEquals("Description", template.description());
        assertEquals("#00AABB", template.color());
        assertEquals(now, template.createdAt());
        verify(repository).create(template);
    }

    @Test
    void deniesNonAdminAndRejectsUnsafeOrInvalidInput() {
        UUID actor = UUID.randomUUID();
        assertThrows(AdminAccessDeniedException.class,
                () -> service.create(actor, "Name", "Description", BadgeIcon.FOOTPRINT, "#000000"));
        verifyNoInteractions(repository);
        when(admins.isAdmin(actor)).thenReturn(true);
        assertThrows(IllegalArgumentException.class,
                () -> service.create(actor, " ", "Description", BadgeIcon.FOOTPRINT, "#000000"));
        assertThrows(IllegalArgumentException.class,
                () -> service.create(actor, "Name", "Description", BadgeIcon.FOOTPRINT, "#fff\"/><script>"));
        assertThrows(IllegalArgumentException.class,
                () -> service.create(actor, "Name", "x".repeat(301), BadgeIcon.FOOTPRINT, "#000000"));
        verifyNoInteractions(repository);
    }

    @Test
    void missingTemplateIsNotFound() {
        assertThrows(BadgeTemplateNotFoundException.class, () -> service.get(UUID.randomUUID()));
    }
}
