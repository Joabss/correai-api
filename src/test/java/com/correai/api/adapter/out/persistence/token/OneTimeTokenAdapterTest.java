package com.correai.api.adapter.out.persistence.token;

import com.correai.api.domain.model.auth.OneTimeTokenType;
import com.correai.api.domain.port.out.auth.OneTimeTokenPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OneTimeTokenAdapterTest {

    @Autowired
    private OneTimeTokenPort tokens;

    @Test
    void consume_shouldReturnOwnerOnlyOnce() {
        UUID userId = UUID.randomUUID();
        String token = tokens.issue(userId, OneTimeTokenType.REFRESH);

        assertEquals(userId, tokens.consume(token, OneTimeTokenType.REFRESH).orElseThrow());
        assertTrue(tokens.consume(token, OneTimeTokenType.REFRESH).isEmpty());
    }

    @Test
    void consume_withWrongTypeOrUnknownToken_shouldBeEmpty() {
        String token = tokens.issue(UUID.randomUUID(), OneTimeTokenType.PASSWORD_RESET);

        assertTrue(tokens.consume(token, OneTimeTokenType.REFRESH).isEmpty());
        assertTrue(tokens.consume("unknown", OneTimeTokenType.PASSWORD_RESET).isEmpty());
        assertTrue(tokens.consume(null, OneTimeTokenType.PASSWORD_RESET).isEmpty());
    }

    @Test
    void revokeAll_shouldInvalidateUserTokens() {
        UUID userId = UUID.randomUUID();
        String first = tokens.issue(userId, OneTimeTokenType.REFRESH);
        String second = tokens.issue(userId, OneTimeTokenType.REFRESH);

        tokens.revokeAll(userId, OneTimeTokenType.REFRESH);

        assertTrue(tokens.consume(first, OneTimeTokenType.REFRESH).isEmpty());
        assertTrue(tokens.consume(second, OneTimeTokenType.REFRESH).isEmpty());
    }
}
