package com.correai.api.domain.model.auth;

import java.time.Instant;
import java.util.UUID;

public record AuthToken(String token, UUID userId, Instant expiresAt, String refreshToken) {

    public AuthToken(String token, UUID userId, Instant expiresAt) {
        this(token, userId, expiresAt, null);
    }

    public AuthToken withRefreshToken(String refreshToken) {
        return new AuthToken(token, userId, expiresAt, refreshToken);
    }
}
