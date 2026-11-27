package com.correai.api.adapter.out.persistence.challenge;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataChallengeParticipationRepository extends JpaRepository<ChallengeParticipationEntity, UUID> {
    Optional<ChallengeParticipationEntity> findByChallengeIdAndUserId(UUID challengeId, UUID userId);
    List<ChallengeParticipationEntity> findByUserId(UUID userId);
    List<ChallengeParticipationEntity> findByUserIdAndFinalizedFalseAndAchievedAtIsNull(UUID userId);
    Page<ChallengeParticipationEntity> findByUserIdAndAchievedAtIsNotNullOrderByAchievedAtDescIdAsc(
            UUID userId, Pageable page);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ChallengeParticipationEntity p where p.challengeId = :challenge and p.userId = :user")
    Optional<ChallengeParticipationEntity> lockParticipation(@Param("challenge") UUID challenge,
                                                              @Param("user") UUID user);
}
