package com.correai.api.adapter.out.persistence.challenge;

import com.correai.api.adapter.out.persistence.user.SpringDataUserRepository;
import com.correai.api.domain.model.challenge.ChallengeConflictException;
import com.correai.api.domain.model.challenge.ChallengeNotFoundException;
import com.correai.api.domain.model.challenge.ChallengeParticipation;
import com.correai.api.domain.port.out.challenge.ChallengeParticipationRepositoryPort;
import com.correai.api.domain.model.pagination.PageQuery;
import com.correai.api.domain.model.pagination.PageResult;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ChallengeParticipationRepositoryAdapter implements ChallengeParticipationRepositoryPort {
    private final SpringDataChallengeParticipationRepository participations;
    private final SpringDataChallengeRepository challenges;
    private final SpringDataUserRepository users;

    public ChallengeParticipationRepositoryAdapter(SpringDataChallengeParticipationRepository participations,
            SpringDataChallengeRepository challenges, SpringDataUserRepository users) {
        this.participations = participations;
        this.challenges = challenges;
        this.users = users;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockUser(UUID userId) {
        users.lockById(userId).orElseThrow(ChallengeNotFoundException::new);
    }

    @Override
    @Transactional
    public ChallengeParticipation create(ChallengeParticipation participation) {
        users.lockById(participation.userId()).orElseThrow(ChallengeNotFoundException::new);
        var challenge = challenges.findById(participation.challengeId()).orElseThrow(ChallengeNotFoundException::new);
        if (challenge.publishedAt == null) {
            throw new ChallengeConflictException("Challenge is not published");
        }
        return participations.findByChallengeIdAndUserId(participation.challengeId(), participation.userId())
                .map(ChallengeParticipationEntity::domain)
                .orElseGet(() -> participations.save(new ChallengeParticipationEntity(participation)).domain());
    }

    @Override
    @Transactional
    public ChallengeParticipation update(ChallengeParticipation expected, ChallengeParticipation revised) {
        if (!expected.challengeId().equals(revised.challengeId()) || !expected.userId().equals(revised.userId())
                || !expected.joinedAt().equals(revised.joinedAt())) {
            throw new IllegalArgumentException("Participation cannot be replaced");
        }
        var entity = participations.lockParticipation(expected.challengeId(), expected.userId())
                .orElseThrow(ChallengeNotFoundException::new);
        if (!entity.domain().equals(expected)) {
            throw new ChallengeConflictException("Challenge participation has changed");
        }
        if ((entity.finalized || entity.achievedAt != null) && !expected.equals(revised)) {
            throw new ChallengeConflictException("Finalized or awarded participation cannot be changed");
        }
        entity.apply(revised);
        return entity.domain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ChallengeParticipation> find(UUID challengeId, UUID userId) {
        return participations.findByChallengeIdAndUserId(challengeId, userId)
                .map(ChallengeParticipationEntity::domain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChallengeParticipation> findByUserId(UUID userId) {
        return participations.findByUserId(userId).stream().map(ChallengeParticipationEntity::domain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChallengeParticipation> findPendingByUserId(UUID userId) {
        return participations.findByUserIdAndFinalizedFalseAndAchievedAtIsNull(userId).stream()
                .map(ChallengeParticipationEntity::domain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ChallengeParticipation> listAwarded(UUID userId, PageQuery page) {
        var result = participations.findByUserIdAndAchievedAtIsNotNullOrderByAchievedAtDescIdAsc(userId,
                PageRequest.of(page.page(), page.size()));
        return new PageResult<>(result.getContent().stream().map(ChallengeParticipationEntity::domain).toList(),
                page.page(), page.size(), result.getTotalElements());
    }
}
