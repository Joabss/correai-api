package com.correai.api.adapter.out.persistence.goal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataGoalRepository extends JpaRepository<GoalEntity, UUID> {

    List<GoalEntity> findByUserIdAndActiveTrueOrderByCreatedAtAsc(UUID userId);

    Optional<GoalEntity> findByIdAndUserId(UUID id, UUID userId);

    List<GoalEntity> findByUserIdAndAchievedAtIsNotNullOrderByAchievedAtDesc(UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("update GoalEntity g set g.achievedAt = :at, g.achievedPeriodStart = :start, "
            + "g.achievedCurrent = :current where g.id = :id and g.userId = :userId "
            + "and g.active = true and g.achievedAt is null")
    int award(UUID id, UUID userId, Instant at, LocalDate start, double current);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("update GoalEntity g set g.active = false where g.id = :id "
            + "and g.userId = :userId and g.achievedAt is null")
    int deactivateUnawarded(UUID id, UUID userId);
}
