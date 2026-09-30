package com.correai.api.application.stats;

import com.correai.api.application.common.StreakCalculator;
import com.correai.api.domain.model.activity.Activity;
import com.correai.api.domain.model.activity.ActivityType;
import com.correai.api.domain.model.activity.PerceivedEffort;
import com.correai.api.domain.model.activity.TrainingType;
import com.correai.api.domain.port.in.stats.StatsSummary;
import com.correai.api.domain.port.in.stats.WeeklyEvolution;
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
class StatsApplicationServiceTest {

    @Mock
    private ActivityRepositoryPort repository;

    private StatsApplicationService service;

    private UUID userId;
    private Activity activity1;
    private Activity activity2;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.systemDefaultZone();
        service = new StatsApplicationService(repository, new StreakCalculator(repository, clock), clock);
        userId = UUID.randomUUID();
        activity1 = Activity.create(userId, ActivityType.RUN, 5.0, 1800, TrainingType.EASY, PerceivedEffort.OK, "Run 1", LocalDate.now());
        activity2 = Activity.create(userId, ActivityType.WALK, 3.0, 1200, TrainingType.EASY, PerceivedEffort.EASY, "Walk 1", LocalDate.now());
    }

    @Test
    void getSummary_shouldReturnStatsSummary() {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(java.time.DayOfWeek.MONDAY);
        LocalDate monthStart = today.withDayOfMonth(1);

        when(repository.findByUserIdAndActivityDateBetween(userId, weekStart, today))
                .thenReturn(List.of(activity1, activity2));
        when(repository.findByUserIdAndActivityDateBetween(userId, monthStart, today))
                .thenReturn(List.of(activity1, activity2));
        when(repository.existsByUserIdAndActivityDate(userId, today)).thenReturn(true);
        when(repository.findLongestDistance(userId)).thenReturn(5.0);

        StatsSummary summary = service.getSummary(userId);

        assertNotNull(summary);
        assertEquals(8.0, summary.kmWeek());
        assertEquals(8.0, summary.kmMonth());
        assertEquals(2, summary.activitiesWeek());
        assertEquals(1, summary.streak());
        assertEquals(5.0, summary.longestDistance());
    }

    @Test
    void getSummary_withNoActivities_shouldReturnZeroValues() {
        when(repository.findByUserIdAndActivityDateBetween(any(UUID.class), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(repository.existsByUserIdAndActivityDate(any(UUID.class), any(LocalDate.class))).thenReturn(false);
        when(repository.findLongestDistance(userId)).thenReturn(null);

        StatsSummary summary = service.getSummary(userId);

        assertNotNull(summary);
        assertEquals(0.0, summary.kmWeek());
        assertEquals(0.0, summary.kmMonth());
        assertEquals(0, summary.activitiesWeek());
        assertEquals(0, summary.streak());
        assertEquals(0.0, summary.longestDistance());
    }

    @Test
    void getEvolution_shouldGroupActivitiesByWeekAndFillEmptyWeeks() {
        Clock fixed = Clock.fixed(java.time.Instant.parse("2026-09-30T12:00:00Z"), java.time.ZoneOffset.UTC);
        StatsApplicationService fixedService =
                new StatsApplicationService(repository, new StreakCalculator(repository, fixed), fixed);
        LocalDate currentWeek = LocalDate.of(2026, 9, 28);
        LocalDate previousWeek = currentWeek.minusWeeks(1);
        when(repository.findByUserIdAndActivityDateBetween(userId, previousWeek.minusWeeks(1), LocalDate.of(2026, 9, 30)))
                .thenReturn(List.of(
                        activityOn(10.0, 3600, LocalDate.of(2026, 9, 29)),
                        activityOn(5.0, 1800, LocalDate.of(2026, 9, 30)),
                        activityOn(4.0, 1440, previousWeek.plusDays(2))));

        List<WeeklyEvolution> evolution = fixedService.getEvolution(userId, 3);

        assertEquals(3, evolution.size());
        assertEquals(previousWeek.minusWeeks(1), evolution.getFirst().weekStart());
        assertEquals(0, evolution.getFirst().activities());
        assertNull(evolution.getFirst().avgPaceSeconds());
        assertEquals(1, evolution.get(1).activities());
        assertEquals(360, evolution.get(1).avgPaceSeconds());
        assertEquals(currentWeek, evolution.get(2).weekStart());
        assertEquals(15.0, evolution.get(2).km());
        assertEquals(2, evolution.get(2).activities());
        assertEquals(360, evolution.get(2).avgPaceSeconds());
    }

    @Test
    void getEvolution_withWeeksOutOfRange_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> service.getEvolution(userId, 0));
        assertThrows(IllegalArgumentException.class, () -> service.getEvolution(userId, 53));
    }

    private Activity activityOn(double km, int seconds, LocalDate date) {
        return Activity.create(userId, ActivityType.RUN, km, seconds, TrainingType.EASY, PerceivedEffort.OK, "n", date);
    }
}
