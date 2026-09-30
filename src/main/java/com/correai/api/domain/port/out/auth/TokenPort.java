package com.correai.api.domain.port.out.auth;

import com.correai.api.domain.model.auth.AuthToken;

import java.util.UUID;

/**
 * Outbound port for issuing and verifying signed access tokens.
 */
public interface TokenPort {

    AuthToken issue(UUID userId);

    /**
     * @return the user id carried by the token
     * @throws com.correai.api.domain.model.auth.InvalidTokenException if the token is malformed, tampered with or expired
     */
    UUID verify(String token);
}
