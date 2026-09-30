package com.correai.api.domain.port.in.stats;

import java.time.LocalDate;

/**
 * Totals for one week (Monday to Sunday). {@code avgPaceSeconds} is null when the week has no distance.
 */
public record WeeklyEvolution(LocalDate weekStart, double km, int activities, Integer avgPaceSeconds) {
}
