package com.correai.api.adapter.out.persistence.goal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataGoalRepository extends JpaRepository<GoalEntity, UUID> {

    List<GoalEntity> findByUserIdAndActiveTrueOrderByCreatedAtAsc(UUID userId);

    Optional<GoalEntity> findByIdAndUserId(UUID id, UUID userId);
}
