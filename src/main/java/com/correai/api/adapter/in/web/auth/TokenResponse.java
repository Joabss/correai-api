package com.correai.api.adapter.in.web.auth;

import com.correai.api.domain.model.auth.AuthToken;

import java.time.Instant;
import java.util.UUID;

public record TokenResponse(String accessToken, String tokenType, UUID userId, Instant expiresAt) {

    public static TokenResponse from(AuthToken token) {
        return new TokenResponse(token.token(), "Bearer", token.userId(), token.expiresAt());
    }
}
