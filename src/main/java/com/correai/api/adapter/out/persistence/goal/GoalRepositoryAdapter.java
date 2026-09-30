package com.correai.api.adapter.out.persistence.goal;

import com.correai.api.domain.model.goal.Goal;
import com.correai.api.domain.port.out.goal.GoalRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class GoalRepositoryAdapter implements GoalRepositoryPort {

    private final SpringDataGoalRepository jpaRepository;
    private final GoalMapper mapper;

    public GoalRepositoryAdapter(SpringDataGoalRepository jpaRepository, GoalMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Goal save(Goal goal) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(goal)));
    }

    @Override
    public List<Goal> findActiveByUserId(UUID userId) {
        return jpaRepository.findByUserIdAndActiveTrueOrderByCreatedAtAsc(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Goal> findByIdAndUserId(UUID goalId, UUID userId) {
        return jpaRepository.findByIdAndUserId(goalId, userId).map(mapper::toDomain);
    }
}
