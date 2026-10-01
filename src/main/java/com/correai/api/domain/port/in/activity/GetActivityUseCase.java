package com.correai.api.domain.port.in.activity;

import com.correai.api.domain.model.activity.Activity;

import java.util.UUID;

/**
 * Inbound port (use case): fetch one activity of a user.
 */
public interface GetActivityUseCase {

    Activity get(UUID userId, UUID activityId);
}
