package com.correai.api.domain.port.in.stats;

/**
 * Aggregated stats for a user, computed from their activities.
 */
public record StatsSummary(
        double kmWeek,
        double kmMonth,
        int activitiesWeek,
        int streak,
        double longestDistance
) {
}

