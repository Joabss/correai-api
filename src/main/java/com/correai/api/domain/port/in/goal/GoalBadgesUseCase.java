package com.correai.api.domain.port.in.goal;

import com.correai.api.domain.model.goal.Goal;
import java.util.List;
import java.util.UUID;

public interface GoalBadgesUseCase {
    List<Goal> listBadges(UUID userId);
    Goal getBadge(UUID userId, UUID goalId);
}
