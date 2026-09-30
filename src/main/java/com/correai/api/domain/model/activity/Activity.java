package com.correai.api.domain.model.activity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Pure domain model for an Activity. No framework/persistence concerns here.
 */
public record Activity(
        UUID id,
        UUID userId,
        ActivityType type,
        LocalDate activityDate,
        Double distanceKm,
        Integer durationSeconds,
        Integer avgPaceSeconds,
        TrainingType trainingType,
        PerceivedEffort perceivedEffort,
        String notes,
        Instant createdAt
) {

    public Activity {
        Objects.requireNonNull(userId);
        Objects.requireNonNull(type);
        if (activityDate == null) {
            activityDate = LocalDate.now();
        }
    }

    /** Creates a brand-new Activity (not yet persisted). */
    public static Activity create(UUID userId, ActivityType type, double distanceKm, int durationSeconds,
                                   TrainingType trainingType, PerceivedEffort perceivedEffort, String notes,
                                   LocalDate activityDate) {
        validate(distanceKm, durationSeconds);
        int pace = calculatePace(distanceKm, durationSeconds);
        return new Activity(null, userId, type, activityDate, distanceKm, durationSeconds, pace,
                trainingType, perceivedEffort, notes, null);
    }

    /** Reconstructs an Activity from persistence. */
    public static Activity reconstruct(UUID id, UUID userId, ActivityType type, LocalDate activityDate,
                                        Double distanceKm, Integer durationSeconds, Integer avgPaceSeconds,
                                        TrainingType trainingType, PerceivedEffort perceivedEffort,
                                        String notes, Instant createdAt) {
        return new Activity(id, userId, type, activityDate, distanceKm, durationSeconds, avgPaceSeconds,
                trainingType, perceivedEffort, notes, createdAt);
    }

    private static void validate(double distanceKm, int durationSeconds) {
        if (distanceKm <= 0) {
            throw new IllegalArgumentException("Distance must be greater than zero");
        }
        if (durationSeconds <= 0) {
            throw new IllegalArgumentException("Duration must be greater than zero");
        }
    }

    private static int calculatePace(double distanceKm, int durationSeconds) {
        return (int) (durationSeconds / distanceKm);
    }

}
