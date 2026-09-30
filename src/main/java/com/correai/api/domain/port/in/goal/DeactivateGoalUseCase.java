package com.correai.api.domain.port.in.goal;

import java.util.UUID;

/**
 * Inbound port (use case): deactivates a goal of the user.
 */
public interface DeactivateGoalUseCase {

    /**
     * @throws com.correai.api.domain.model.goal.GoalNotFoundException if the goal does not exist or belongs to another user
     */
    void deactivate(UUID userId, UUID goalId);
}
