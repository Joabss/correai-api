package com.correai.api.domain.activity;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ActivityTest {
    private final UUID userId = UUID.randomUUID();

    @Test
    void create_shouldCreateActivityWithCorrectValues() {
        Activity activity = Activity.create(userId, ActivityType.RUN, 10.0, 3600, TrainingType.EASY, PerceivedEffort.OK, "Test");

        assertNotNull(activity);
        assertEquals(userId, activity.getUserId());
        assertEquals(ActivityType.RUN, activity.getType());
        assertEquals(LocalDate.now(), activity.getActivityDate());
        assertEquals(10.0, activity.getDistanceKm());
        assertEquals(3600, activity.getDurationSeconds());
        assertEquals(360, activity.getAvgPaceSeconds()); // 3600 / 10
        assertEquals(TrainingType.EASY, activity.getTrainingType());
        assertEquals(PerceivedEffort.OK, activity.getPerceivedEffort());
        assertEquals("Test", activity.getNotes());
        assertNotNull(activity.getCreatedAt());
    }

    @Test
    void create_withZeroDistance_shouldThrowException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Activity.create(userId, ActivityType.RUN, 0.0, 3600, TrainingType.EASY, PerceivedEffort.OK, "Test"));
        assertEquals("Distance must be greater than zero", exception.getMessage());
    }

    @Test
    void create_withNegativeDistance_shouldThrowException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Activity.create(userId, ActivityType.RUN, -1.0, 3600, TrainingType.EASY, PerceivedEffort.OK, "Test"));
        assertEquals("Distance must be greater than zero", exception.getMessage());
    }

    @Test
    void create_withZeroDuration_shouldThrowException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Activity.create(userId, ActivityType.RUN, 10.0, 0, TrainingType.EASY, PerceivedEffort.OK, "Test"));
        assertEquals("Duration must be greater than zero", exception.getMessage());
    }

    @Test
    void create_withNegativeDuration_shouldThrowException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Activity.create(userId, ActivityType.RUN, 10.0, -1, TrainingType.EASY, PerceivedEffort.OK, "Test"));
        assertEquals("Duration must be greater than zero", exception.getMessage());
    }

    @Test
    void create_withNullUserId_shouldThrowException() {
        NullPointerException exception = assertThrows(NullPointerException.class,
                () -> Activity.create(null, ActivityType.RUN, 10.0, 3600, TrainingType.EASY, PerceivedEffort.OK, "Test"));
        assertNotNull(exception);
    }

    @Test
    void create_withNullType_shouldThrowException() {
        NullPointerException exception = assertThrows(NullPointerException.class,
                () -> Activity.create(userId, null, 10.0, 3600, TrainingType.EASY, PerceivedEffort.OK, "Test"));
        assertNotNull(exception);
    }

    @Test
    void prePersist_shouldSetCreatedAt() throws Exception {
        Activity activity = Activity.create(userId, ActivityType.RUN, 10.0, 3600, TrainingType.EASY, PerceivedEffort.OK, "Test");

        // Set createdAt to null to simulate not set
        Field createdAtField = Activity.class.getDeclaredField("createdAt");
        createdAtField.setAccessible(true);
        createdAtField.set(activity, null);

        // Call prePersist
        Method prePersistMethod = Activity.class.getDeclaredMethod("prePersist");
        prePersistMethod.setAccessible(true);
        prePersistMethod.invoke(activity);

        // Assert createdAt is set
        assertNotNull(activity.getCreatedAt());
    }
}
