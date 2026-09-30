package com.correai.api.domain.model.auth;

import java.time.Instant;
import java.util.UUID;

public record AuthToken(String token, UUID userId, Instant expiresAt) {
}
