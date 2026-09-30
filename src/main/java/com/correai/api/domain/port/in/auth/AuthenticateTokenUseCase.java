package com.correai.api.domain.port.in.auth;

import java.util.UUID;

/**
 * Inbound port (use case): resolves the user identified by an access token.
 */
public interface AuthenticateTokenUseCase {

    /**
     * @throws com.correai.api.domain.model.auth.InvalidTokenException if the token is invalid, expired or the user no longer exists
     */
    UUID authenticate(String token);
}
