package com.correai.api.domain.port.in.admin;

import java.util.UUID;

public interface AdminUseCase {
    void bootstrap(UUID userId);
    void grant(UUID actorId, UUID targetId);
    void revoke(UUID actorId, UUID targetId);
    boolean isAdmin(UUID userId);
}
