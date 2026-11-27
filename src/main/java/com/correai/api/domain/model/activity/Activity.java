package com.correai.api.domain.model.activity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
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
        LocalTime startTime,
        Instant startedAt,
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
        return create(userId, type, distanceKm, durationSeconds, trainingType, perceivedEffort, notes,
                activityDate, null, null);
    }

    public static Activity createWithStartedAt(UUID userId, ActivityType type, double distanceKm,
                                               int durationSeconds, TrainingType trainingType,
                                               PerceivedEffort perceivedEffort, String notes,
                                               OffsetDateTime startedAt) {
        return create(userId, type, distanceKm, durationSeconds, trainingType, perceivedEffort, notes,
                startedAt.toLocalDate(), startedAt.toLocalTime(), startedAt.toInstant());
    }

    private static Activity create(UUID userId, ActivityType type, double distanceKm, int durationSeconds,
                                    TrainingType trainingType, PerceivedEffort perceivedEffort, String notes,
                                    LocalDate activityDate, LocalTime startTime, Instant startedAt) {
        validate(distanceKm, durationSeconds);
        int pace = calculatePace(distanceKm, durationSeconds);
        return new Activity(null, userId, type, activityDate, startTime, startedAt, distanceKm, durationSeconds,
                pace, trainingType, perceivedEffort, notes, null);
    }

    /** Reconstructs an Activity from persistence. */
    public static Activity reconstruct(UUID id, UUID userId, ActivityType type, LocalDate activityDate,
                                        Double distanceKm, Integer durationSeconds, Integer avgPaceSeconds,
                                        TrainingType trainingType, PerceivedEffort perceivedEffort,
                                        String notes, Instant createdAt) {
        return reconstructWithStartedAt(id, userId, type, activityDate, null, null, distanceKm,
                durationSeconds, avgPaceSeconds, trainingType, perceivedEffort, notes, createdAt);
    }

    public static Activity reconstructWithStartedAt(UUID id, UUID userId, ActivityType type,
                                                     LocalDate activityDate, LocalTime startTime,
                                                     Instant startedAt, Double distanceKm,
                                                     Integer durationSeconds, Integer avgPaceSeconds,
                                                     TrainingType trainingType, PerceivedEffort perceivedEffort,
                                                     String notes, Instant createdAt) {
        return new Activity(id, userId, type, activityDate, startTime, startedAt, distanceKm, durationSeconds,
                avgPaceSeconds, trainingType, perceivedEffort, notes, createdAt);
    }

    public Activity update(ActivityType type, double distanceKm, int durationSeconds,
                           TrainingType trainingType, PerceivedEffort perceivedEffort, String notes) {
        validate(distanceKm, durationSeconds);
        return new Activity(id, userId, type, activityDate, startTime, startedAt, distanceKm, durationSeconds,
                calculatePace(distanceKm, durationSeconds), trainingType, perceivedEffort, notes, createdAt);
    }

    private static void validate(double distanceKm, int durationSeconds) {
        if (!Double.isFinite(distanceKm) || distanceKm <= 0) {
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
