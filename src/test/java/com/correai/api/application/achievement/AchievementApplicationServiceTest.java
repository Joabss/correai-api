package com.correai.api.application.achievement;

import com.correai.api.domain.model.achievement.BadgeCode;
import com.correai.api.domain.model.activity.Activity;
import com.correai.api.domain.model.activity.ActivityType;
import com.correai.api.domain.model.activity.PerceivedEffort;
import com.correai.api.domain.model.activity.TrainingType;
import com.correai.api.domain.model.goal.Goal;
import com.correai.api.domain.model.goal.GoalPeriod;
import com.correai.api.domain.model.goal.GoalType;
import com.correai.api.domain.port.in.stats.GetStatsSummaryUseCase;
import com.correai.api.domain.port.in.stats.PersonalRecordsUseCase;
import com.correai.api.domain.port.in.stats.StatsSummary;
import com.correai.api.domain.port.out.achievement.AchievementRepositoryPort;
import com.correai.api.domain.port.out.activity.ActivityRepositoryPort;
import com.correai.api.domain.port.out.goal.GoalRepositoryPort;
import com.correai.api.domain.port.out.media.MediaRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AchievementApplicationServiceTest {

    @Mock
    private ActivityRepositoryPort activities;
    @Mock
    private GetStatsSummaryUseCase stats;
    @Mock
    private PersonalRecordsUseCase personalRecords;
    @Mock
    private GoalRepositoryPort goals;
    @Mock
    private MediaRepositoryPort media;
    @Mock
    private AchievementRepositoryPort repository;

    private AchievementApplicationService service;
    private final UUID userId = UUID.randomUUID();
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-28T12:00:00Z"), ZoneId.of("UTC"));

    @BeforeEach
    void setUp() {
        service = new AchievementApplicationService(activities, stats, personalRecords, goals, media, repository, clock);
        lenient().when(stats.getSummary(userId)).thenReturn(zeroSummary());
        lenient().when(activities.findAllByUserId(userId)).thenReturn(List.of());
        lenient().when(personalRecords.records(userId)).thenReturn(List.of());
        lenient().when(goals.findAwardedByUserId(userId)).thenReturn(List.of());
        lenient().when(media.listRouteActivityIds(userId)).thenReturn(List.of());
        lenient().when(repository.findUnlockedByUserId(userId)).thenReturn(Map.of());
        lenient().when(repository.grantIfAbsent(any(), any(), any())).thenReturn(true);
    }

    private static StatsSummary zeroSummary() {
        return new StatsSummary(0, 0, 0, 0, 0);
    }

    @Test
    void unlocksDistanceAndStreakAndMonthlyBadgesFromStatsSummary() {
        when(stats.getSummary(userId)).thenReturn(new StatsSummary(0, 100, 0, 30, 42.195));

        List<BadgeCode> granted = service.evaluate(userId);

        assertTrue(granted.containsAll(List.of(BadgeCode.PRIMEIRO_PASSO, BadgeCode.CORRIDA_5K,
                BadgeCode.CORRIDA_10K, BadgeCode.MEIA_MARATONA, BadgeCode.MARATONA,
                BadgeCode.SEQUENCIA_7, BadgeCode.SEQUENCIA_30, BadgeCode.CENTURIAO)));
        assertFalse(granted.contains(BadgeCode.RELAMPAGO));
        assertFalse(granted.contains(BadgeCode.RECORDE_PESSOAL));
    }

    @Test
    void attributesDistanceBadgesToTheActivityDateNotTheEvaluationMoment() {
        LocalDate backdated = LocalDate.of(2026, 11, 26);
        Activity manualEntry = Activity.reconstruct(UUID.randomUUID(), userId, ActivityType.RUN, backdated,
                5.0, 1800, 360, TrainingType.EASY, PerceivedEffort.OK, null, Instant.now(clock));
        when(stats.getSummary(userId)).thenReturn(new StatsSummary(0, 0, 0, 0, 5.0));
        when(activities.findAllByUserId(userId)).thenReturn(List.of(manualEntry));

        service.evaluate(userId);

        Instant expected = backdated.atTime(LocalTime.NOON).atZone(ZoneOffset.UTC).toInstant();
        verify(repository).grantIfAbsent(userId, BadgeCode.PRIMEIRO_PASSO, expected);
        verify(repository).grantIfAbsent(userId, BadgeCode.CORRIDA_5K, expected);
        assertFalse(expected.equals(Instant.now(clock)), "must not fall back to the evaluation instant");
    }

    @Test
    void correctsAnAlreadyUnlockedBadgeWhenAnEarlierAttributionIsFound() {
        LocalDate backdated = LocalDate.of(2026, 11, 26);
        Activity manualEntry = Activity.reconstruct(UUID.randomUUID(), userId, ActivityType.RUN, backdated,
                5.0, 1800, 360, TrainingType.EASY, PerceivedEffort.OK, null, Instant.now(clock));
        when(stats.getSummary(userId)).thenReturn(new StatsSummary(0, 0, 0, 0, 5.0));
        when(activities.findAllByUserId(userId)).thenReturn(List.of(manualEntry));
        // Simulates a badge already granted with the stale "evaluation moment" timestamp, the bug
        // this fix addresses.
        Instant staleUnlockedAt = Instant.now(clock);
        when(repository.findUnlockedByUserId(userId)).thenReturn(Map.of(BadgeCode.PRIMEIRO_PASSO, staleUnlockedAt));

        List<BadgeCode> granted = service.evaluate(userId);

        Instant correctInstant = backdated.atTime(LocalTime.NOON).atZone(ZoneOffset.UTC).toInstant();
        assertFalse(granted.contains(BadgeCode.PRIMEIRO_PASSO),
                "correcting a date must not be reported as a newly granted badge");
        verify(repository, never()).grantIfAbsent(eq(userId), eq(BadgeCode.PRIMEIRO_PASSO), any());
        verify(repository).correctUnlockedAt(userId, BadgeCode.PRIMEIRO_PASSO, correctInstant);
    }

    @Test
    void unlocksTimeOfDayBadgesFromActivityStartTimes() {
        when(activities.findAllByUserId(userId)).thenReturn(List.of(
                activityAt(LocalTime.of(5, 0)),
                activityAt(LocalTime.of(22, 0)),
                activityAt(LocalTime.of(12, 0))));

        List<BadgeCode> granted = service.evaluate(userId);

        assertTrue(granted.containsAll(List.of(BadgeCode.MADRUGADOR, BadgeCode.CORUJA, BadgeCode.SOL_A_PINO)));
    }

    @Test
    void unlocksRecordePessoalWhenAnyPersonalRecordExists() {
        when(personalRecords.records(userId)).thenReturn(List.of(
                new PersonalRecordsUseCase.PersonalRecord(5.0,
                        List.of(new PersonalRecordsUseCase.Effort(UUID.randomUUID(), 1500)))));

        List<BadgeCode> granted = service.evaluate(userId);

        assertTrue(granted.contains(BadgeCode.RECORDE_PESSOAL));
    }

    @Test
    void unlocksRelampagoWhenAnAvgPaceGoalWasAwarded() {
        Goal goal = Goal.reconstruct(UUID.randomUUID(), userId, GoalType.AVG_PACE, GoalPeriod.WEEKLY, 300, true,
                Instant.now(clock)).achieve(Instant.now(clock), LocalDate.now(clock), 280);
        when(goals.findAwardedByUserId(userId)).thenReturn(List.of(goal));

        List<BadgeCode> granted = service.evaluate(userId);

        assertTrue(granted.contains(BadgeCode.RELAMPAGO));
    }

    @Test
    void unlocksCoracaoForteAfterFourConsecutiveWeeklyActivityGoals() {
        LocalDate firstMonday = LocalDate.of(2026, 10, 5);
        List<Goal> weeklyGoals = List.of(
                weeklyActivitiesGoal(firstMonday),
                weeklyActivitiesGoal(firstMonday.plusWeeks(1)),
                weeklyActivitiesGoal(firstMonday.plusWeeks(2)),
                weeklyActivitiesGoal(firstMonday.plusWeeks(3)));
        when(goals.findAwardedByUserId(userId)).thenReturn(weeklyGoals);

        List<BadgeCode> granted = service.evaluate(userId);

        assertTrue(granted.contains(BadgeCode.CORACAO_FORTE));
    }

    @Test
    void doesNotUnlockCoracaoForteWhenWeeksAreNotConsecutive() {
        LocalDate firstMonday = LocalDate.of(2026, 10, 5);
        List<Goal> weeklyGoals = List.of(
                weeklyActivitiesGoal(firstMonday),
                weeklyActivitiesGoal(firstMonday.plusWeeks(1)),
                weeklyActivitiesGoal(firstMonday.plusWeeks(3)),
                weeklyActivitiesGoal(firstMonday.plusWeeks(4)));
        when(goals.findAwardedByUserId(userId)).thenReturn(weeklyGoals);

        List<BadgeCode> granted = service.evaluate(userId);

        assertFalse(granted.contains(BadgeCode.CORACAO_FORTE));
    }

    @Test
    void neverUnlocksBadgesWithoutActiveCriteria() {
        when(stats.getSummary(userId)).thenReturn(new StatsSummary(100, 1000, 100, 365, 42.195));
        when(activities.findAllByUserId(userId)).thenReturn(List.of(
                activityAt(LocalTime.of(5, 0)), activityAt(LocalTime.of(22, 0))));
        when(personalRecords.records(userId)).thenReturn(List.of(
                new PersonalRecordsUseCase.PersonalRecord(5.0,
                        List.of(new PersonalRecordsUseCase.Effort(UUID.randomUUID(), 1500)))));

        List<BadgeCode> granted = service.evaluate(userId);

        assertFalse(granted.contains(BadgeCode.REI_DA_MONTANHA));
        assertFalse(granted.contains(BadgeCode.CHUVA));
        assertFalse(granted.contains(BadgeCode.EM_GRUPO));
        assertFalse(granted.contains(BadgeCode.CAMPEAO));
    }

    @Test
    void evaluateDoesNotRegrantAlreadyUnlockedBadges() {
        when(stats.getSummary(userId)).thenReturn(new StatsSummary(0, 0, 0, 0, 5.0));
        Map<BadgeCode, Instant> unlocked = new EnumMap<>(BadgeCode.class);
        unlocked.put(BadgeCode.PRIMEIRO_PASSO, Instant.now(clock));
        unlocked.put(BadgeCode.CORRIDA_5K, Instant.now(clock));
        when(repository.findUnlockedByUserId(userId)).thenReturn(unlocked);

        List<BadgeCode> granted = service.evaluate(userId);

        assertTrue(granted.isEmpty());
        verify(repository, never()).grantIfAbsent(userId, BadgeCode.PRIMEIRO_PASSO, Instant.now(clock));
        verify(repository, never()).grantIfAbsent(userId, BadgeCode.CORRIDA_5K, Instant.now(clock));
    }

    @Test
    void listReturnsFullCatalogWithUnlockedState() {
        Instant unlockedAt = Instant.now(clock);
        when(stats.getSummary(userId)).thenReturn(zeroSummary());
        when(repository.findUnlockedByUserId(userId)).thenReturn(Map.of(BadgeCode.PRIMEIRO_PASSO, unlockedAt));

        var result = service.list(userId);

        assertEquals(BadgeCode.values().length, result.size());
        assertTrue(result.stream().filter(a -> a.code() == BadgeCode.PRIMEIRO_PASSO).findFirst().orElseThrow().unlocked());
        assertFalse(result.stream().filter(a -> a.code() == BadgeCode.MARATONA).findFirst().orElseThrow().unlocked());
    }

    @Test
    void unlocksExploradorWhenRouteStartsAreFarApart() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        when(media.listRouteActivityIds(userId)).thenReturn(List.of(a, b));
        when(media.find(userId, a, com.correai.api.domain.model.media.MediaKind.ACTIVITY_ROUTE))
                .thenReturn(java.util.Optional.of(routeStartingAt(0.0, 0.0)));
        when(media.find(userId, b, com.correai.api.domain.model.media.MediaKind.ACTIVITY_ROUTE))
                .thenReturn(java.util.Optional.of(routeStartingAt(5.0, 5.0)));

        List<BadgeCode> granted = service.evaluate(userId);

        assertTrue(granted.contains(BadgeCode.EXPLORADOR));
    }

    @Test
    void doesNotUnlockExploradorWhenAllRoutesStartNearby() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        when(media.listRouteActivityIds(userId)).thenReturn(List.of(a, b));
        when(media.find(userId, a, com.correai.api.domain.model.media.MediaKind.ACTIVITY_ROUTE))
                .thenReturn(java.util.Optional.of(routeStartingAt(0.0, 0.0)));
        when(media.find(userId, b, com.correai.api.domain.model.media.MediaKind.ACTIVITY_ROUTE))
                .thenReturn(java.util.Optional.of(routeStartingAt(0.0001, 0.0001)));

        List<BadgeCode> granted = service.evaluate(userId);

        assertFalse(granted.contains(BadgeCode.EXPLORADOR));
    }

    private static com.correai.api.domain.model.media.MediaContent routeStartingAt(double lat, double lon) {
        String xml = "<gpx xmlns=\"http://www.topografix.com/GPX/1/1\"><trk><trkseg>"
                + "<trkpt lat=\"" + lat + "\" lon=\"" + lon + "\"></trkpt>"
                + "<trkpt lat=\"" + lat + "\" lon=\"" + (lon + 0.01) + "\"></trkpt></trkseg></trk></gpx>";
        return new com.correai.api.domain.model.media.MediaContent("application/gpx+xml",
                xml.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private Goal weeklyActivitiesGoal(LocalDate periodStart) {
        return Goal.reconstruct(UUID.randomUUID(), userId, GoalType.ACTIVITIES, GoalPeriod.WEEKLY, 3, true,
                        Instant.now(clock))
                .achieve(Instant.now(clock), periodStart, 3);
    }

    private Activity activityAt(LocalTime startTime) {
        return Activity.reconstructWithStartedAt(UUID.randomUUID(), userId, ActivityType.RUN,
                LocalDate.now(clock), startTime,
                LocalDateTime.of(LocalDate.now(clock), startTime).atZone(clock.getZone()).toInstant(),
                5.0, 1800, 360, TrainingType.EASY, PerceivedEffort.OK, null, Instant.now(clock));
    }
}
