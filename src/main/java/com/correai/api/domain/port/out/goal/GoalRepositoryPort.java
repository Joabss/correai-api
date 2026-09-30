package com.correai.api.domain.port.out.goal;

import com.correai.api.domain.model.goal.Goal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for Goal persistence, implemented by an infrastructure adapter.
 */
public interface GoalRepositoryPort {

    Goal save(Goal goal);

    List<Goal> findActiveByUserId(UUID userId);

    Optional<Goal> findByIdAndUserId(UUID goalId, UUID userId);
}
