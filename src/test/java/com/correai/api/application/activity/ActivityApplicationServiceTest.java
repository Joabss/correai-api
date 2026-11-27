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
import com.correai.api.domain.port.in.activity.UpdateActivityCommand;
import com.correai.api.domain.model.activity.ActivityNotFoundException;
import com.correai.api.domain.port.out.activity.ActivityRepositoryPort;
import com.correai.api.domain.port.in.goal.EvaluateGoalsUseCase;
import com.correai.api.domain.port.in.achievement.AchievementsUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityApplicationServiceTest {

    @Mock
    private ActivityRepositoryPort repository;
    @Mock
    private EvaluateGoalsUseCase goals;
    @Mock
    private AchievementsUseCase achievements;

    private ActivityApplicationService service;

    private UUID userId;
    private CreateActivityCommand command;
    private Activity activity;
    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-11-28T01:00:00Z"), ZoneId.of("America/Sao_Paulo"));
        service = new ActivityApplicationService(repository, new StreakCalculator(repository, clock), clock, goals,
                achievements);
        userId = UUID.randomUUID();
        command = new CreateActivityCommand(userId, ActivityType.RUN, 10.0, 3600, TrainingType.EASY,
                PerceivedEffort.OK, "Test run", null);
        activity = Activity.reconstructWithStartedAt(UUID.randomUUID(), userId, ActivityType.RUN, LocalDate.now(clock),
                LocalTime.of(22, 0), Instant.now(), 10.0, 3600, 360,
                TrainingType.EASY, PerceivedEffort.OK, "Test run", java.time.Instant.now());
    }

    @Test
    void updateSavesExistingActivityWithRecalculatedPace() {
        when(repository.findByIdAndUserId(activity.id(), userId)).thenReturn(java.util.Optional.of(activity));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Activity result = service.update(new UpdateActivityCommand(userId, activity.id(), ActivityType.WALK,
                5, 2400, null, null, "updated"));
        assertEquals(activity.id(), result.id());
        assertEquals(activity.startedAt(), result.startedAt());
        assertEquals(activity.activityDate(), result.activityDate());
        assertEquals(activity.createdAt(), result.createdAt());
        assertEquals(480, result.avgPaceSeconds());
        assertEquals("updated", result.notes());
        verify(repository).save(result);
        var order = inOrder(goals, repository);
        order.verify(goals).evaluate(userId);
        order.verify(repository).save(result);
        order.verify(goals).evaluate(userId);
    }

    @Test
    void updateRejectsMissingOrForeignActivityWithoutSaving() {
        assertThrows(ActivityNotFoundException.class, () -> service.update(new UpdateActivityCommand(
                userId, UUID.randomUUID(), ActivityType.RUN, 1, 300, null, null, null)));
        verify(repository, never()).save(any());
        verifyNoInteractions(goals);
    }

    @Test
    void create_shouldSaveActivityAndReturnResult() {
        when(repository.save(any(Activity.class))).thenReturn(activity);
        when(repository.findByUserIdAndActivityDateBetween(any(UUID.class), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(activity));
        when(repository.existsByUserIdAndActivityDate(eq(userId), any(LocalDate.class)))
                .thenAnswer(invocation -> {
                    LocalDate date = invocation.getArgument(1);
                    return date.equals(LocalDate.now(clock));
                });

        ActivityCreationResult result = service.create(command);

        assertNotNull(result);
        assertEquals(activity.id(), result.activity().id());
        assertEquals(10.0, result.totalKmMonth());
        assertEquals(1, result.streak());
        verify(repository).save(any(Activity.class));
    }

    @Test
    void create_shouldUseSaoPauloDateAndTimeWhenStartIsNotProvided() {
        when(repository.save(any(Activity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findByUserIdAndActivityDateBetween(any(UUID.class), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(repository.existsByUserIdAndActivityDate(eq(userId), any(LocalDate.class))).thenReturn(false);

        service.create(command);

        var savedActivity = org.mockito.ArgumentCaptor.forClass(Activity.class);
        verify(repository).save(savedActivity.capture());
        assertEquals(LocalDate.of(2026, 11, 27), savedActivity.getValue().activityDate());
        assertEquals(LocalTime.of(22, 0), savedActivity.getValue().startTime());
    }

    @Test
    void create_shouldPreserveProvidedOffsetAndPersistInstant() {
        OffsetDateTime startedAt = OffsetDateTime.of(2026, 6, 1, 4, 30, 0, 0, ZoneOffset.ofHours(9));
        CreateActivityCommand commandWithStart = new CreateActivityCommand(userId, ActivityType.RUN, 10.0, 3600,
                TrainingType.EASY, PerceivedEffort.OK, "Test run", startedAt);
        when(repository.save(any(Activity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findByUserIdAndActivityDateBetween(any(UUID.class), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(repository.existsByUserIdAndActivityDate(eq(userId), any(LocalDate.class))).thenReturn(false);

        service.create(commandWithStart);

        var savedActivity = org.mockito.ArgumentCaptor.forClass(Activity.class);
        verify(repository).save(savedActivity.capture());
        assertEquals(startedAt.toLocalDate(), savedActivity.getValue().activityDate());
        assertEquals(startedAt.toLocalTime(), savedActivity.getValue().startTime());
        assertEquals(startedAt.toInstant(), savedActivity.getValue().startedAt());
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
    void get_shouldReturnActivityOfUser() {
        when(repository.findByIdAndUserId(activity.id(), userId)).thenReturn(java.util.Optional.of(activity));

        assertEquals(activity, service.get(userId, activity.id()));
    }

    @Test
    void get_unknownActivity_shouldThrowNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndUserId(id, userId)).thenReturn(java.util.Optional.empty());

        assertThrows(com.correai.api.domain.model.activity.ActivityNotFoundException.class,
                () -> service.get(userId, id));
    }

    @Test
    void delete_shouldRemoveActivity() {
        when(repository.findByIdAndUserId(activity.id(), userId)).thenReturn(java.util.Optional.of(activity));

        service.delete(userId, activity.id());

        verify(repository).delete(activity);
    }

    @Test
    void delete_unknownActivity_shouldNotDelete() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndUserId(id, userId)).thenReturn(java.util.Optional.empty());

        assertThrows(com.correai.api.domain.model.activity.ActivityNotFoundException.class,
                () -> service.delete(userId, id));
        verify(repository, never()).delete(any());
    }

    @Test
    void create_withInvalidDistance_shouldThrowException() {
        CreateActivityCommand invalid = new CreateActivityCommand(userId, ActivityType.RUN, 0.0, 3600,
                TrainingType.EASY, PerceivedEffort.OK, "Test", null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.create(invalid));
        assertEquals("Distance must be greater than zero", exception.getMessage());
    }

    @Test
    void create_withInvalidDuration_shouldThrowException() {
        CreateActivityCommand invalid = new CreateActivityCommand(userId, ActivityType.RUN, 10.0, 0,
                TrainingType.EASY, PerceivedEffort.OK, "Test", null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.create(invalid));
        assertEquals("Duration must be greater than zero", exception.getMessage());
    }
}
