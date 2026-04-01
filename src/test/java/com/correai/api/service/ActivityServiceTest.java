package com.correai.api.service;

import com.correai.api.domain.activity.Activity;
import com.correai.api.domain.activity.ActivityType;
import com.correai.api.domain.activity.PerceivedEffort;
import com.correai.api.domain.activity.TrainingType;
import com.correai.api.dto.ActivityItemResponse;
import com.correai.api.dto.ActivityRequest;
import com.correai.api.dto.ActivityResponse;
import com.correai.api.repository.ActivityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

    @Mock
    private ActivityRepository repository;

    @InjectMocks
    private ActivityService service;

    private UUID userId;
    private ActivityRequest request;
    private Activity activity;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        request = new ActivityRequest();
        request.setType(ActivityType.RUN);
        request.setDistanceKm(10.0);
        request.setDurationSeconds(3600);
        request.setTrainingType(TrainingType.EASY);
        request.setPerceivedEffort(PerceivedEffort.OK);
        request.setNotes("Test run");

        activity = Activity.create(userId, ActivityType.RUN, 10.0, 3600, TrainingType.EASY, PerceivedEffort.OK, "Test run");
    }

    @Test
    void create_shouldSaveActivityAndReturnResponse() {
        when(repository.save(any(Activity.class))).thenReturn(activity);
        when(repository.findByUserIdAndActivityDateBetween(any(UUID.class), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(activity));
        when(repository.existsByUserIdAndActivityDate(eq(userId), any(LocalDate.class)))
                .thenAnswer(invocation -> {
                    LocalDate date = invocation.getArgument(1);
                    return date.equals(LocalDate.now());
                });

        ActivityResponse response = service.create(userId, request);

        assertNotNull(response);
        assertEquals(activity.getId(), response.getId());
        assertEquals("06:00", response.getAvgPace()); // 3600 / 10 = 360 seconds = 6:00
        assertEquals(10.0, response.getTotalKmMonth());
        assertEquals(1, response.getStreak());
        verify(repository).save(any(Activity.class));
    }

    @Test
    void list_shouldReturnListOfActivityItemResponses() {
        when(repository.findByUserIdOrderByActivityDateDesc(userId)).thenReturn(List.of(activity));

        List<ActivityItemResponse> responses = service.list(userId);

        assertEquals(1, responses.size());
        ActivityItemResponse item = responses.get(0);
        assertEquals(activity.getId(), item.getId());
        assertEquals(ActivityType.RUN, item.getType());
        assertEquals(activity.getActivityDate(), item.getDate());
        assertEquals(10.0, item.getDistanceKm());
        assertEquals(3600, item.getDurationSeconds());
        assertEquals("06:00", item.getAvgPace());
    }

    @Test
    void create_withInvalidDistance_shouldThrowException() {
        request.setDistanceKm(0.0);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.create(userId, request));
        assertEquals("Distance must be greater than zero", exception.getMessage());
    }

    @Test
    void create_withInvalidDuration_shouldThrowException() {
        request.setDurationSeconds(0);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.create(userId, request));
        assertEquals("Duration must be greater than zero", exception.getMessage());
    }
}
