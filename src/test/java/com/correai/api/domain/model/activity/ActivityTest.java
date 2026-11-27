package com.correai.api.domain.model.activity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ActivityTest {
    private final UUID userId = UUID.randomUUID();

    @Test
    void updatePreservesIdentityAndTimestampsAndRecalculatesPace() {
        Activity original = Activity.reconstructWithStartedAt(UUID.randomUUID(), userId, ActivityType.RUN,
                LocalDate.of(2026, 11, 27), LocalTime.of(22, 0), Instant.parse("2026-11-28T01:00:00Z"),
                10.0, 3600, 360, TrainingType.EASY, PerceivedEffort.OK, "before", Instant.EPOCH);
        Activity updated = original.update(ActivityType.WALK, 5, 2400, null, null, null);
        assertEquals(original.id(), updated.id());
        assertEquals(original.userId(), updated.userId());
        assertEquals(original.activityDate(), updated.activityDate());
        assertEquals(original.startTime(), updated.startTime());
        assertEquals(original.startedAt(), updated.startedAt());
        assertEquals(original.createdAt(), updated.createdAt());
        assertEquals(ActivityType.WALK, updated.type());
        assertEquals(480, updated.avgPaceSeconds());
        assertNull(updated.trainingType());
        assertNull(updated.perceivedEffort());
        assertNull(updated.notes());
        assertEquals(10.0, original.distanceKm());
    }

    @Test
    void updateRejectsInvalidDistanceDurationAndType() {
        Activity original = Activity.create(userId, ActivityType.RUN, 1, 300, null, null, null, LocalDate.now());
        for (double distance : new double[] {0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class,
                    () -> original.update(ActivityType.RUN, distance, 300, null, null, null));
        }
        assertThrows(IllegalArgumentException.class,
                () -> original.update(ActivityType.RUN, 1, 0, null, null, null));
        assertThrows(NullPointerException.class, () -> original.update(null, 1, 300, null, null, null));
        Activity legacy = original.update(ActivityType.WALK, 2, 300, null, null, null);
        assertNull(legacy.startedAt());
        assertNull(legacy.startTime());
        assertEquals(original.activityDate(), legacy.activityDate());
    }

    @Test
    void create_shouldCreateActivityWithCorrectValues() {
        Activity activity = Activity.create(userId, ActivityType.RUN, 10.0, 3600, TrainingType.EASY, PerceivedEffort.OK, "Test", LocalDate.now());

        assertNotNull(activity);
        assertNull(activity.id());
        assertEquals(userId, activity.userId());
        assertEquals(ActivityType.RUN, activity.type());
        assertEquals(LocalDate.now(), activity.activityDate());
        assertEquals(10.0, activity.distanceKm());
        assertEquals(3600, activity.durationSeconds());
        assertEquals(360, activity.avgPaceSeconds()); // 3600 / 10
        assertEquals(TrainingType.EASY, activity.trainingType());
        assertEquals(PerceivedEffort.OK, activity.perceivedEffort());
        assertEquals("Test", activity.notes());
    }

    @Test
    void create_withZeroDistance_shouldThrowException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Activity.create(userId, ActivityType.RUN, 0.0, 3600, TrainingType.EASY, PerceivedEffort.OK, "Test", LocalDate.now()));
        assertEquals("Distance must be greater than zero", exception.getMessage());
    }

    @Test
    void create_withNegativeDistance_shouldThrowException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Activity.create(userId, ActivityType.RUN, -1.0, 3600, TrainingType.EASY, PerceivedEffort.OK, "Test", LocalDate.now()));
        assertEquals("Distance must be greater than zero", exception.getMessage());
    }

    @Test
    void create_withZeroDuration_shouldThrowException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Activity.create(userId, ActivityType.RUN, 10.0, 0, TrainingType.EASY, PerceivedEffort.OK, "Test", LocalDate.now()));
        assertEquals("Duration must be greater than zero", exception.getMessage());
    }

    @Test
    void create_withNegativeDuration_shouldThrowException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Activity.create(userId, ActivityType.RUN, 10.0, -1, TrainingType.EASY, PerceivedEffort.OK, "Test", LocalDate.now()));
        assertEquals("Duration must be greater than zero", exception.getMessage());
    }

    @Test
    void create_shouldKeepStartDateAndTime() {
        OffsetDateTime startedAt = OffsetDateTime.of(2026, 11, 27, 22, 0, 0, 0, ZoneOffset.ofHours(-3));

        Activity activity = Activity.createWithStartedAt(userId, ActivityType.RUN, 5.0, 1500,
                TrainingType.EASY, PerceivedEffort.OK, "Test", startedAt);

        assertEquals(startedAt.toLocalDate(), activity.activityDate());
        assertEquals(startedAt.toLocalTime(), activity.startTime());
        assertEquals(startedAt.toInstant(), activity.startedAt());
    }

    @Test
    void create_withNullUserId_shouldThrowException() {
        assertThrows(NullPointerException.class,
                () -> Activity.create(null, ActivityType.RUN, 10.0, 3600, TrainingType.EASY, PerceivedEffort.OK, "Test", LocalDate.now()));
    }

    @Test
    void create_withNullType_shouldThrowException() {
        assertThrows(NullPointerException.class,
                () -> Activity.create(userId, null, 10.0, 3600, TrainingType.EASY, PerceivedEffort.OK, "Test", LocalDate.now()));
    }

    @Test
    void reconstruct_shouldRestoreAllFields() {
        UUID id = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 1, 1);
        var createdAt = Instant.now();
        var startedAt = Instant.parse("2026-01-01T07:15:00Z");

        Activity activity = Activity.reconstructWithStartedAt(id, userId, ActivityType.WALK, date, LocalTime.of(8, 15),
                startedAt, 5.0, 1800, 360,
                TrainingType.LONG, PerceivedEffort.EASY, "note", createdAt);

        assertEquals(id, activity.id());
        assertEquals(userId, activity.userId());
        assertEquals(ActivityType.WALK, activity.type());
        assertEquals(date, activity.activityDate());
        assertEquals(LocalTime.of(8, 15), activity.startTime());
        assertEquals(startedAt, activity.startedAt());
        assertEquals(5.0, activity.distanceKm());
        assertEquals(1800, activity.durationSeconds());
        assertEquals(360, activity.avgPaceSeconds());
        assertEquals(TrainingType.LONG, activity.trainingType());
        assertEquals(PerceivedEffort.EASY, activity.perceivedEffort());
        assertEquals("note", activity.notes());
        assertEquals(createdAt, activity.createdAt());
    }
}
