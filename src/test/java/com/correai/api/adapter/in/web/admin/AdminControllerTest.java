package com.correai.api.adapter.in.web.admin;

import com.correai.api.domain.model.admin.AdminAccessDeniedException;
import com.correai.api.domain.port.in.admin.AdminUseCase;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class AdminControllerTest {
    @Test
    void bootstrapDisabledWhenSecretIsMissingOrTooShort() {
        var admins = mock(AdminUseCase.class);
        var user = UUID.randomUUID();
        assertThrows(AdminAccessDeniedException.class, () -> new AdminController(admins, "").bootstrap(user, ""));
        assertThrows(AdminAccessDeniedException.class, () -> new AdminController(admins, "short").bootstrap(user, "short"));
        verifyNoInteractions(admins);
    }
}
