package com.correai.api.domain.port.in.auth;

import com.correai.api.domain.model.auth.AuthToken;

/**
 * Inbound port (use case): authenticates an existing user by e-mail and password.
 */
public interface LoginUseCase {

    AuthToken login(String email, String password);
}
