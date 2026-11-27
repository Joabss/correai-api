package com.correai.api.adapter.out.persistence.challenge;

import com.correai.api.domain.model.challenge.Challenge;
import com.correai.api.domain.model.challenge.ChallengeConflictException;
import com.correai.api.domain.model.challenge.ChallengeNotFoundException;
import com.correai.api.domain.model.pagination.PageQuery;
import com.correai.api.domain.model.pagination.PageResult;
import com.correai.api.domain.port.out.challenge.ChallengeRepositoryPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class ChallengeRepositoryAdapter implements ChallengeRepositoryPort {
    private final SpringDataChallengeRepository repository;

    public ChallengeRepositoryAdapter(SpringDataChallengeRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Challenge create(Challenge challenge) {
        return repository.save(new ChallengeEntity(challenge)).domain();
    }

    @Override
    @Transactional
    public Challenge saveDraft(Challenge expected, Challenge revised) {
        if (!expected.id().equals(revised.id()) || !expected.creatorId().equals(revised.creatorId())
                || !expected.createdAt().equals(revised.createdAt()) || revised.publishedAt() != null) {
            throw new IllegalArgumentException("Invalid challenge revision");
        }
        ChallengeEntity entity = repository.lockOwn(expected.id(), expected.creatorId())
                .orElseThrow(ChallengeNotFoundException::new);
        if (entity.publishedAt != null) {
            throw new ChallengeConflictException("Published challenges cannot be edited");
        }
        if (!entity.domain().equals(expected)) {
            throw new ChallengeConflictException("Challenge draft has changed");
        }
        entity.apply(revised);
        return entity.domain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Challenge> findById(UUID id) {
        return repository.findById(id).map(ChallengeEntity::domain);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Challenge> lockOwn(UUID userId, UUID id) {
        return repository.lockOwn(id, userId).map(ChallengeEntity::domain);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Challenge publish(Challenge expected, Instant now) {
        var entity = repository.lockOwn(expected.id(), expected.creatorId())
                .orElseThrow(ChallengeNotFoundException::new);
        if (!entity.domain().equals(expected)) {
            throw new ChallengeConflictException("Challenge draft has changed");
        }

        Challenge published = expected.publish(now);
        entity.apply(published);
        return entity.domain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Challenge> findVisible(UUID userId, UUID id) {
        return repository.findVisible(userId, id).map(ChallengeEntity::domain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Challenge> listVisible(UUID userId, PageQuery page) {
        var result = repository.listVisible(userId, PageRequest.of(page.page(), page.size()));
        return new PageResult<>(result.getContent().stream().map(ChallengeEntity::domain).toList(),
                page.page(), page.size(), result.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Challenge> findOwn(UUID userId, UUID id) {
        return repository.findByIdAndCreatorId(id, userId).map(ChallengeEntity::domain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Challenge> listOwn(UUID userId, PageQuery page) {
        var result = repository.findByCreatorIdAndPublishedAtIsNullOrderByCreatedAtDescIdAsc(userId,
                PageRequest.of(page.page(), page.size()));
        return new PageResult<>(result.getContent().stream().map(ChallengeEntity::domain).toList(),
                page.page(), page.size(), result.getTotalElements());
    }
}
