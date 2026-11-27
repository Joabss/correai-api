package com.correai.api.application.goal;

import com.correai.api.domain.model.activity.Activity;
import com.correai.api.domain.model.activity.ActivityType;
import com.correai.api.domain.model.activity.PerceivedEffort;
import com.correai.api.domain.model.activity.TrainingType;
import com.correai.api.domain.model.goal.Goal;
import com.correai.api.domain.model.goal.GoalNotFoundException;
import com.correai.api.domain.model.goal.GoalHasBadgeException;
import com.correai.api.domain.model.goal.GoalPeriod;
import com.correai.api.domain.model.goal.GoalType;
import com.correai.api.domain.port.in.goal.CreateGoalCommand;
import com.correai.api.domain.port.in.goal.GoalProgress;
import com.correai.api.domain.port.out.activity.ActivityRepositoryPort;
import com.correai.api.domain.port.out.goal.GoalRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoalApplicationServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 30);
    private static final LocalDate WEEK_START = LocalDate.of(2026, 9, 28);
    private static final LocalDate MONTH_START = LocalDate.of(2026, 9, 1);

    @Mock
    private GoalRepositoryPort goalRepository;

    @Mock
    private ActivityRepositoryPort activityRepository;

    private GoalApplicationService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-30T12:00:00Z"), ZoneOffset.UTC);
        service = new GoalApplicationService(goalRepository, activityRepository, clock);
        userId = UUID.randomUUID();
    }

    @Test
    void create_shouldDeactivateActiveGoalOfSameKindOnly() {
        Goal sameKind = Goal.reconstruct(UUID.randomUUID(), userId, GoalType.DISTANCE_KM, GoalPeriod.WEEKLY, 20, true, Instant.now());
        Goal otherKind = Goal.reconstruct(UUID.randomUUID(), userId, GoalType.ACTIVITIES, GoalPeriod.WEEKLY, 3, true, Instant.now());
        when(goalRepository.findActiveByUserId(userId)).thenReturn(List.of(sameKind, otherKind));
        when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Goal created = service.create(new CreateGoalCommand(userId, GoalType.DISTANCE_KM, GoalPeriod.WEEKLY, 30));

        ArgumentCaptor<Goal> saved = ArgumentCaptor.forClass(Goal.class);
        verify(goalRepository).save(saved.capture());
        verify(goalRepository).deactivateUnawarded(sameKind.deactivate());
        assertTrue(created.active());
        assertEquals(30, created.target());
    }

    @Test
    void listActive_distanceGoal_shouldReportProgressAndCapPercentage() {
        Goal goal = Goal.reconstruct(UUID.randomUUID(), userId, GoalType.DISTANCE_KM, GoalPeriod.WEEKLY, 10, true, Instant.now());
        when(goalRepository.findActiveByUserId(userId)).thenReturn(List.of(goal));
        when(activityRepository.findByUserIdAndActivityDateBetween(userId, WEEK_START, TODAY))
                .thenReturn(List.of(activity(12.0, 3600)));
        when(goalRepository.award(any())).thenAnswer(invocation -> invocation.getArgument(0));

        GoalProgress progress = service.listActive(userId).getFirst();

        assertEquals(12.0, progress.current());
        assertEquals(100.0, progress.percentage());
        assertTrue(progress.achieved());
        assertEquals(WEEK_START, progress.periodStart());
        assertEquals(LocalDate.of(2026, 10, 4), progress.periodEnd());
    }

    @Test
    void listActive_activitiesGoal_shouldCountActivities() {
        Goal goal = Goal.reconstruct(UUID.randomUUID(), userId, GoalType.ACTIVITIES, GoalPeriod.MONTHLY, 4, true, Instant.now());
        when(goalRepository.findActiveByUserId(userId)).thenReturn(List.of(goal));
        when(activityRepository.findByUserIdAndActivityDateBetween(userId, MONTH_START, TODAY))
                .thenReturn(List.of(activity(5.0, 1500), activity(5.0, 1500)));

        GoalProgress progress = service.listActive(userId).getFirst();

        assertEquals(2.0, progress.current());
        assertEquals(50.0, progress.percentage());
        assertFalse(progress.achieved());
    }

    @Test
    void listActive_paceGoal_isAchievedWhenPaceIsAtOrBelowTarget() {
        Goal goal = Goal.reconstruct(UUID.randomUUID(), userId, GoalType.AVG_PACE, GoalPeriod.WEEKLY, 330, true, Instant.now());
        when(goalRepository.findActiveByUserId(userId)).thenReturn(List.of(goal));
        when(activityRepository.findByUserIdAndActivityDateBetween(userId, WEEK_START, TODAY))
                .thenReturn(List.of(activity(10.0, 3000)));
        when(goalRepository.award(any())).thenAnswer(invocation -> invocation.getArgument(0));

        GoalProgress progress = service.listActive(userId).getFirst();

        assertEquals(300.0, progress.current());
        assertNull(progress.percentage());
        assertTrue(progress.achieved());
    }

    @Test
    void listActive_paceGoalWithoutActivities_shouldNotBeAchieved() {
        Goal goal = Goal.reconstruct(UUID.randomUUID(), userId, GoalType.AVG_PACE, GoalPeriod.WEEKLY, 330, true, Instant.now());
        when(goalRepository.findActiveByUserId(userId)).thenReturn(List.of(goal));
        when(activityRepository.findByUserIdAndActivityDateBetween(userId, WEEK_START, TODAY)).thenReturn(List.of());

        GoalProgress progress = service.listActive(userId).getFirst();

        assertNull(progress.current());
        assertFalse(progress.achieved());
    }

    @Test
    void paceComparisonDoesNotRoundDownToAwardAnUnmetChallenge() {
        Goal goal = Goal.reconstruct(UUID.randomUUID(), userId, GoalType.AVG_PACE,
                GoalPeriod.WEEKLY, 330, true, Instant.now());
        when(goalRepository.findActiveByUserId(userId)).thenReturn(List.of(goal));
        when(activityRepository.findByUserIdAndActivityDateBetween(userId, WEEK_START, TODAY))
                .thenReturn(List.of(activity(10, 3309)));
        assertFalse(service.listActive(userId).getFirst().achieved());
        verify(goalRepository, never()).award(any());
    }

    @Test
    void fractionalPaceBelowOneSecondRemainsPositiveAndCanBeAwarded() {
        Goal goal = Goal.reconstruct(UUID.randomUUID(), userId, GoalType.AVG_PACE,
                GoalPeriod.WEEKLY, 330, true, Instant.now());
        when(goalRepository.findActiveByUserId(userId)).thenReturn(List.of(goal));
        when(activityRepository.findByUserIdAndActivityDateBetween(userId, WEEK_START, TODAY))
                .thenReturn(List.of(activity(10, 1)));
        when(goalRepository.award(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var progress = service.listActive(userId).getFirst();
        assertTrue(progress.achieved());
        assertEquals(0.1, progress.current());
    }

    @Test
    void deactivate_shouldSaveInactiveGoal() {
        Goal goal = Goal.reconstruct(UUID.randomUUID(), userId, GoalType.ACTIVITIES, GoalPeriod.WEEKLY, 3, true, Instant.now());
        when(goalRepository.findByIdAndUserId(goal.id(), userId)).thenReturn(Optional.of(goal));

        service.deactivate(userId, goal.id());

        ArgumentCaptor<Goal> saved = ArgumentCaptor.forClass(Goal.class);
        verify(goalRepository).deactivateUnawarded(saved.capture());
        assertFalse(saved.getValue().active());
    }

    @Test
    void deactivate_withUnknownGoal_shouldThrowNotFound() {
        UUID goalId = UUID.randomUUID();
        when(goalRepository.findByIdAndUserId(goalId, userId)).thenReturn(Optional.empty());

        assertThrows(GoalNotFoundException.class, () -> service.deactivate(userId, goalId));
        verify(goalRepository, never()).save(any());
    }

    @Test
    void achievementSurvivesLowerProgressAndNewPeriodAndCannotBeDeleted() {
        Goal original = Goal.reconstruct(UUID.randomUUID(), userId, GoalType.DISTANCE_KM,
                GoalPeriod.WEEKLY, 10, true, Instant.parse("2026-09-01T00:00:00Z"));
        Goal awarded = original.achieve(Instant.parse("2026-09-29T12:00:00Z"), WEEK_START, 12);
        when(goalRepository.findActiveByUserId(userId)).thenReturn(List.of(awarded));
        when(goalRepository.findByIdAndUserId(awarded.id(), userId)).thenReturn(Optional.of(awarded));
        service = new GoalApplicationService(goalRepository, activityRepository,
                Clock.fixed(Instant.parse("2026-10-07T12:00:00Z"), ZoneOffset.UTC));

        GoalProgress progress = service.listActive(userId).getFirst();
        assertTrue(progress.achieved());
        assertEquals(12, progress.current());
        assertEquals(WEEK_START, progress.periodStart());
        assertEquals(awarded.achievedAt(), progress.goal().achievedAt());
        assertThrows(GoalHasBadgeException.class, () -> service.deactivate(userId, awarded.id()));
        verifyNoInteractions(activityRepository);
        verify(goalRepository, never()).deactivateUnawarded(any());
        verify(goalRepository, never()).award(any());
    }

    @Test
    void replacingSameKindKeepsAwardedChallenge() {
        Goal awarded = Goal.reconstruct(UUID.randomUUID(), userId, GoalType.ACTIVITIES,
                GoalPeriod.WEEKLY, 1, true, Instant.now())
                .achieve(Instant.parse("2026-09-29T12:00:00Z"), WEEK_START, 1);
        when(goalRepository.findActiveByUserId(userId)).thenReturn(List.of(awarded));
        when(goalRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service.create(new CreateGoalCommand(userId, GoalType.ACTIVITIES, GoalPeriod.WEEKLY, 3));
        verify(goalRepository, never()).deactivateUnawarded(any());
    }

    @Test
    void deletingPreviouslyUnobservedCompletedChallengeAwardsAndRejectsDeletion() {
        Goal goal = Goal.reconstruct(UUID.randomUUID(), userId, GoalType.ACTIVITIES,
                GoalPeriod.WEEKLY, 1, true, Instant.now());
        when(goalRepository.findByIdAndUserId(goal.id(), userId)).thenReturn(Optional.of(goal));
        when(activityRepository.findByUserIdAndActivityDateBetween(userId, WEEK_START, TODAY))
                .thenReturn(List.of(activity(1, 300)));
        when(goalRepository.award(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertThrows(GoalHasBadgeException.class, () -> service.deactivate(userId, goal.id()));
        verify(goalRepository).award(any());
        verify(goalRepository, never()).deactivateUnawarded(any());
    }

    private Activity activity(double km, int seconds) {
        return Activity.create(userId, ActivityType.RUN, km, seconds, TrainingType.EASY, PerceivedEffort.OK, "n", TODAY);
    }
}
