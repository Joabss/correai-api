package com.correai.api.domain.port.out.admin;

import java.time.Instant;
import java.util.UUID;

public interface AdminRepositoryPort {
    void bootstrap(UUID userId, Instant now);
    void grant(UUID actorId, UUID targetId, Instant now);
    void revoke(UUID actorId, UUID targetId, Instant now);
    boolean isAdmin(UUID userId);
    boolean isAdminLocked(UUID userId);
}
