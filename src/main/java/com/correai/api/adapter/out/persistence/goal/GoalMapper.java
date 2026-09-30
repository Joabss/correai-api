package com.correai.api.adapter.out.persistence.goal;

import com.correai.api.domain.model.goal.Goal;
import org.springframework.stereotype.Component;

@Component
public class GoalMapper {

    public GoalEntity toEntity(Goal goal) {
        return new GoalEntity(goal.id(), goal.userId(), goal.type(), goal.period(),
                goal.target(), goal.active(), goal.createdAt());
    }

    public Goal toDomain(GoalEntity entity) {
        return Goal.reconstruct(entity.getId(), entity.getUserId(), entity.getType(), entity.getPeriod(),
                entity.getTarget(), entity.isActive(), entity.getCreatedAt());
    }
}
