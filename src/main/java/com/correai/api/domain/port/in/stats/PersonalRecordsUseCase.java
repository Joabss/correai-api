package com.correai.api.domain.port.in.stats;

import java.util.List;
import java.util.UUID;

public interface PersonalRecordsUseCase {
    List<PersonalRecord> records(UUID userId);

    record PersonalRecord(double distanceKm, List<Effort> efforts) {
    }

    record Effort(UUID activityId, double durationSeconds) {
    }
}
