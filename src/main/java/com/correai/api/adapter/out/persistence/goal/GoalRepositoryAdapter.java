package com.correai.api.adapter.out.persistence.goal;

import com.correai.api.domain.model.goal.Goal;
import com.correai.api.domain.model.goal.GoalHasBadgeException;
import com.correai.api.domain.model.goal.GoalNotFoundException;
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
    public Goal award(Goal goal) {
        jpaRepository.award(goal.id(), goal.userId(), goal.achievedAt(),
                goal.achievedPeriodStart(), goal.achievedCurrent());
        return findByIdAndUserId(goal.id(), goal.userId())
                .orElseThrow(() -> new GoalNotFoundException(goal.id()));
    }

    @Override
    public void deactivateUnawarded(Goal goal) {
        if (jpaRepository.deactivateUnawarded(goal.id(), goal.userId()) == 0) {
            Goal current = findByIdAndUserId(goal.id(), goal.userId())
                    .orElseThrow(() -> new GoalNotFoundException(goal.id()));
            if (current.hasBadge()) throw new GoalHasBadgeException();
        }
    }

    @Override
    public List<Goal> findAwardedByUserId(UUID userId) {
        return jpaRepository.findByUserIdAndAchievedAtIsNotNullOrderByAchievedAtDesc(userId)
                .stream().map(mapper::toDomain).toList();
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
