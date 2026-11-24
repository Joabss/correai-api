package com.correai.api.domain.port.out.auth;

import com.correai.api.domain.model.auth.OneTimeTokenType;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for opaque, single-use tokens (refresh and password reset).
 */
public interface OneTimeTokenPort {

    /** Issues a new token valid for {@code type.ttl()} and returns its plain value. */
    String issue(UUID userId, OneTimeTokenType type);

    /** Atomically consumes the token; returns the owner only if it existed and was not expired. */
    Optional<UUID> consume(String token, OneTimeTokenType type);

    void revokeAll(UUID userId, OneTimeTokenType type);
}
