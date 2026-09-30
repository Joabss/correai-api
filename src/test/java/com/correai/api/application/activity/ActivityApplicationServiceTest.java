package com.correai.api.application.activity;

import com.correai.api.application.common.StreakCalculator;
import com.correai.api.domain.model.activity.Activity;
import com.correai.api.domain.model.activity.ActivityType;
import com.correai.api.domain.model.activity.PerceivedEffort;
import com.correai.api.domain.model.activity.TrainingType;
import com.correai.api.domain.model.pagination.PageQuery;
import com.correai.api.domain.model.pagination.PageResult;
import com.correai.api.domain.port.in.activity.ActivityCreationResult;
import com.correai.api.domain.port.in.activity.CreateActivityCommand;
import com.correai.api.domain.port.out.activity.ActivityRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityApplicationServiceTest {

    @Mock
    private ActivityRepositoryPort repository;

    private ActivityApplicationService service;

    private UUID userId;
    private CreateActivityCommand command;
    private Activity activity;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.systemDefaultZone();
        service = new ActivityApplicationService(repository, new StreakCalculator(repository, clock), clock);
        userId = UUID.randomUUID();
        command = new CreateActivityCommand(userId, ActivityType.RUN, 10.0, 3600, TrainingType.EASY, PerceivedEffort.OK, "Test run");
        activity = Activity.reconstruct(UUID.randomUUID(), userId, ActivityType.RUN, LocalDate.now(), 10.0, 3600, 360,
                TrainingType.EASY, PerceivedEffort.OK, "Test run", java.time.Instant.now());
    }

    @Test
    void create_shouldSaveActivityAndReturnResult() {
        when(repository.save(any(Activity.class))).thenReturn(activity);
        when(repository.findByUserIdAndActivityDateBetween(any(UUID.class), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(activity));
        when(repository.existsByUserIdAndActivityDate(eq(userId), any(LocalDate.class)))
                .thenAnswer(invocation -> {
                    LocalDate date = invocation.getArgument(1);
                    return date.equals(LocalDate.now());
                });

        ActivityCreationResult result = service.create(command);

        assertNotNull(result);
        assertEquals(activity.id(), result.activity().id());
        assertEquals(10.0, result.totalKmMonth());
        assertEquals(1, result.streak());
        verify(repository).save(any(Activity.class));
    }

    @Test
    void list_shouldReturnActivities() {
        PageQuery pageQuery = new PageQuery(0, 20);
        when(repository.findByUserIdOrderByActivityDateDesc(userId, pageQuery))
                .thenReturn(new PageResult<>(List.of(activity), 0, 20, 1));

        PageResult<Activity> activities = service.list(userId, pageQuery);

        assertEquals(1, activities.content().size());
        assertEquals(activity.id(), activities.content().getFirst().id());
        assertEquals(1, activities.totalPages());
    }

    @Test
    void create_withInvalidDistance_shouldThrowException() {
        CreateActivityCommand invalid = new CreateActivityCommand(userId, ActivityType.RUN, 0.0, 3600, TrainingType.EASY, PerceivedEffort.OK, "Test");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.create(invalid));
        assertEquals("Distance must be greater than zero", exception.getMessage());
    }

    @Test
    void create_withInvalidDuration_shouldThrowException() {
        CreateActivityCommand invalid = new CreateActivityCommand(userId, ActivityType.RUN, 10.0, 0, TrainingType.EASY, PerceivedEffort.OK, "Test");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.create(invalid));
        assertEquals("Duration must be greater than zero", exception.getMessage());
    }
}

