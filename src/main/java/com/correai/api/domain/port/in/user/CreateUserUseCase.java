package com.correai.api.domain.port.in.user;

import com.correai.api.domain.model.auth.AuthToken;

/**
 * Inbound port (use case): registers a user with e-mail and password and issues an access token.
 */
public interface CreateUserUseCase {

    AuthToken create(String name, String email, String password);
}
