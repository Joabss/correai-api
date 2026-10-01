package com.correai.api.domain.port.in.activity;

import java.util.UUID;

/**
 * Inbound port (use case): delete one activity of a user.
 */
public interface DeleteActivityUseCase {

    void delete(UUID userId, UUID activityId);
}
