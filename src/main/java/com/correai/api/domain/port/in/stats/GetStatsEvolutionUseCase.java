package com.correai.api.domain.port.in.stats;

import java.util.List;
import java.util.UUID;

/**
 * Inbound port (use case): weekly evolution of a user, from the oldest to the current week.
 */
public interface GetStatsEvolutionUseCase {

    int MAX_WEEKS = 52;

    /**
     * @param weeks number of weeks to return, current week included (1 to {@link #MAX_WEEKS})
     * @throws IllegalArgumentException if {@code weeks} is out of range
     */
    List<WeeklyEvolution> getEvolution(UUID userId, int weeks);
}
