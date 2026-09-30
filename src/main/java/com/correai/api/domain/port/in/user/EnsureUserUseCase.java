package com.correai.api.domain.port.in.user;

import java.util.UUID;

/**
 * Inbound port (use case): resolves the current user, creating an anonymous one when needed.
 */
public interface EnsureUserUseCase {

    /**
     * @param userIdOrNull the user id provided by the client, or {@code null} if not provided
     * @return the resolved user id (existing, or a newly created anonymous one)
     */
    UUID resolveOrCreate(UUID userIdOrNull);
}

