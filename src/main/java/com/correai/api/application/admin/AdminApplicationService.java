package com.correai.api.application.admin;

import com.correai.api.domain.port.in.admin.AdminUseCase;
import com.correai.api.domain.port.out.admin.AdminRepositoryPort;
import java.time.Clock;
import java.util.UUID;

public class AdminApplicationService implements AdminUseCase {
    private final AdminRepositoryPort admins;
    private final Clock clock;

    public AdminApplicationService(AdminRepositoryPort admins, Clock clock) {
        this.admins = admins;
        this.clock = clock;
    }

    @Override
    public void bootstrap(UUID userId) {
        admins.bootstrap(userId, clock.instant());
    }

    @Override
    public void grant(UUID actorId, UUID targetId) {
        admins.grant(actorId, targetId, clock.instant());
    }

    @Override
    public void revoke(UUID actorId, UUID targetId) {
        admins.revoke(actorId, targetId, clock.instant());
    }

    @Override
    public boolean isAdmin(UUID userId) {
        return admins.isAdmin(userId);
    }
}
