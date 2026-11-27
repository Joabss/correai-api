package com.correai.api.domain.port.out.challenge;

import com.correai.api.domain.model.challenge.Challenge;
import com.correai.api.domain.model.pagination.PageQuery;
import com.correai.api.domain.model.pagination.PageResult;
import java.util.Optional;
import java.time.Instant;
import java.util.UUID;

public interface ChallengeRepositoryPort {
    Challenge create(Challenge challenge);
    Challenge saveDraft(Challenge expected, Challenge revised);
    Optional<Challenge> findById(UUID id);
    Optional<Challenge> lockOwn(UUID userId, UUID id);
    Challenge publish(Challenge expected, Instant now);
    Optional<Challenge> findVisible(UUID userId, UUID id);
    PageResult<Challenge> listVisible(UUID userId, PageQuery page);
    Optional<Challenge> findOwn(UUID userId, UUID id);
    PageResult<Challenge> listOwn(UUID userId, PageQuery page);
}
