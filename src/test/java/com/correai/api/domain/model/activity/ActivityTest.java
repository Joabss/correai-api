package com.correai.api.domain.model.activity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ActivityTest {
    private final UUID userId = UUID.randomUUID();

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
        var createdAt = java.time.Instant.now();

        Activity activity = Activity.reconstruct(id, userId, ActivityType.WALK, date, 5.0, 1800, 360,
                TrainingType.LONG, PerceivedEffort.EASY, "note", createdAt);

        assertEquals(id, activity.id());
        assertEquals(userId, activity.userId());
        assertEquals(ActivityType.WALK, activity.type());
        assertEquals(date, activity.activityDate());
        assertEquals(5.0, activity.distanceKm());
        assertEquals(1800, activity.durationSeconds());
        assertEquals(360, activity.avgPaceSeconds());
        assertEquals(TrainingType.LONG, activity.trainingType());
        assertEquals(PerceivedEffort.EASY, activity.perceivedEffort());
        assertEquals("note", activity.notes());
        assertEquals(createdAt, activity.createdAt());
    }
}

