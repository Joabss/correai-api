package com.correai.api.domain.port.in.goal;

import java.util.List;
import java.util.UUID;

/**
 * Inbound port (use case): lists the active goals of a user with their progress in the current period.
 */
public interface ListGoalsUseCase {

    List<GoalProgress> listActive(UUID userId);
}
