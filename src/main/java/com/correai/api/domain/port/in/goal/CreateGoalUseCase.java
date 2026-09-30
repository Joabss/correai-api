package com.correai.api.domain.port.in.goal;

import com.correai.api.domain.model.goal.Goal;

/**
 * Inbound port (use case): creates a goal, replacing the active goal of the same type and period.
 */
public interface CreateGoalUseCase {

    Goal create(CreateGoalCommand command);
}
