package com.correai.api.adapter.out.persistence.token;

import com.correai.api.domain.model.auth.OneTimeTokenType;
import com.correai.api.domain.port.out.auth.OneTimeTokenPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Component
public class OneTimeTokenAdapter implements OneTimeTokenPort {

    private static final int TOKEN_BYTES = 32;

    private final SpringDataOneTimeTokenRepository repository;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public OneTimeTokenAdapter(SpringDataOneTimeTokenRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public String issue(UUID userId, OneTimeTokenType type) {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = Instant.now(clock).plus(type.ttl());
        repository.save(new OneTimeTokenEntity(null, hash(token), userId, type.name(), expiresAt));
        return token;
    }

    @Override
    @Transactional
    public Optional<UUID> consume(String token, OneTimeTokenType type) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return repository.findByTokenHashAndType(hash(token), type.name())
                .filter(entity -> repository.deleteByIdReturningCount(entity.getId()) == 1)
                .filter(entity -> entity.getExpiresAt().isAfter(Instant.now(clock)))
                .map(OneTimeTokenEntity::getUserId);
    }

    @Override
    @Transactional
    public void revokeAll(UUID userId, OneTimeTokenType type) {
        repository.deleteByUserIdAndType(userId, type.name());
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
