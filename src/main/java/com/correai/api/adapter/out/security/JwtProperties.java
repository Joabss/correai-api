package com.correai.api.adapter.out.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(String secret, Duration expiration) {

    private static final int MIN_SECRET_BYTES = 32;

    public JwtProperties {
        if (secret == null || secret.getBytes().length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException(
                    "app.security.jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes long");
        }
        if (expiration == null) {
            expiration = Duration.ofDays(30);
        }
    }
}
