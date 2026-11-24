package com.correai.api.domain.port.in.user;

import com.correai.api.domain.model.user.User;

import java.util.UUID;

/**
 * Inbound port (use case): fetches a user by id.
 */
public interface GetUserUseCase {

    User getById(UUID userId);
}
