package com.correai.api.domain.port.in.goal;

import com.correai.api.domain.model.goal.Goal;

import java.time.LocalDate;

/**
 * Progress of a goal in the current period. {@code current} is null for pace goals with no distance yet,
 * and {@code percentage} (0-100, capped) is null for pace goals.
 */
public record GoalProgress(
        Goal goal,
        LocalDate periodStart,
        LocalDate periodEnd,
        Double current,
        Double percentage,
        boolean achieved
) {
}
