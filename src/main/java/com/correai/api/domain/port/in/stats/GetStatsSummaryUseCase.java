package com.correai.api.domain.port.in.stats;

import java.util.UUID;

/**
 * Inbound port (use case): get the stats summary for a user.
 */
public interface GetStatsSummaryUseCase {

    StatsSummary getSummary(UUID userId);
}

