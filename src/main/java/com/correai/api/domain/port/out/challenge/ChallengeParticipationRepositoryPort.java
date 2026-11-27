package com.correai.api.domain.port.out.challenge;

import com.correai.api.domain.model.challenge.ChallengeParticipation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.correai.api.domain.model.pagination.PageQuery;
import com.correai.api.domain.model.pagination.PageResult;

public interface ChallengeParticipationRepositoryPort {
    void lockUser(UUID userId);
    ChallengeParticipation create(ChallengeParticipation participation);
    ChallengeParticipation update(ChallengeParticipation expected, ChallengeParticipation revised);
    Optional<ChallengeParticipation> find(UUID challengeId, UUID userId);
    List<ChallengeParticipation> findByUserId(UUID userId);
    List<ChallengeParticipation> findPendingByUserId(UUID userId);
    PageResult<ChallengeParticipation> listAwarded(UUID userId, PageQuery page);
}
