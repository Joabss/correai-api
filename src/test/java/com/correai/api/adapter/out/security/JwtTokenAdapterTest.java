package com.correai.api.adapter.out.security;

import com.correai.api.domain.model.auth.AuthToken;
import com.correai.api.domain.model.auth.InvalidTokenException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenAdapterTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-0123";

    private final JwtTokenAdapter adapter = new JwtTokenAdapter(new JwtProperties(SECRET, Duration.ofHours(1)));

    @Test
    void issueAndVerify_shouldRoundTripUserId() {
        UUID userId = UUID.randomUUID();

        AuthToken token = adapter.issue(userId);

        assertEquals(userId, token.userId());
        assertEquals(userId, adapter.verify(token.token()));
    }

    @Test
    void verify_withTokenSignedByAnotherSecret_shouldThrow() {
        JwtTokenAdapter other = new JwtTokenAdapter(
                new JwtProperties("another-secret-another-secret-0123456789", Duration.ofHours(1)));
        AuthToken token = other.issue(UUID.randomUUID());

        assertThrows(InvalidTokenException.class, () -> adapter.verify(token.token()));
    }

    @Test
    void verify_withExpiredToken_shouldThrow() {
        Clock past = Clock.fixed(Instant.now().minus(Duration.ofDays(2)), ZoneOffset.UTC);
        JwtTokenAdapter expiring = new JwtTokenAdapter(new JwtProperties(SECRET, Duration.ofHours(1)), past);
        AuthToken token = expiring.issue(UUID.randomUUID());

        assertThrows(InvalidTokenException.class, () -> adapter.verify(token.token()));
    }

    @Test
    void verify_withMalformedToken_shouldThrow() {
        assertThrows(InvalidTokenException.class, () -> adapter.verify("not-a-jwt"));
    }

    @Test
    void properties_withShortSecret_shouldFail() {
        assertThrows(IllegalArgumentException.class, () -> new JwtProperties("short", Duration.ofHours(1)));
    }
}
