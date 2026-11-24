package com.correai.api.domain.model.auth;

import java.time.Duration;

public enum OneTimeTokenType {

    REFRESH(Duration.ofDays(30)),
    PASSWORD_RESET(Duration.ofHours(1));

    private final Duration ttl;

    OneTimeTokenType(Duration ttl) {
        this.ttl = ttl;
    }

    public Duration ttl() {
        return ttl;
    }
}
