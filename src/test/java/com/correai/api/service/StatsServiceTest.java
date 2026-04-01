package com.correai.api.service;

import com.correai.api.domain.activity.Activity;
import com.correai.api.domain.activity.ActivityType;
import com.correai.api.domain.activity.PerceivedEffort;
import com.correai.api.domain.activity.TrainingType;
import com.correai.api.dto.StatsSummaryResponse;
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
class StatsServiceTest {

    @Mock
    private ActivityRepository repository;

    @InjectMocks
    private StatsService service;

    private UUID userId;
    private Activity activity1;
    private Activity activity2;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        activity1 = Activity.create(userId, ActivityType.RUN, 5.0, 1800, TrainingType.EASY, PerceivedEffort.OK, "Run 1");
        activity2 = Activity.create(userId, ActivityType.WALK, 3.0, 1200, TrainingType.EASY, PerceivedEffort.EASY, "Walk 1");
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

        StatsSummaryResponse response = service.getSummary(userId);

        assertNotNull(response);
        assertEquals(8.0, response.getKmWeek()); // 5 + 3
        assertEquals(8.0, response.getKmMonth());
        assertEquals(2, response.getActivitiesWeek());
        assertEquals(1, response.getStreak());
        assertEquals(5.0, response.getLongestDistance());
    }

    @Test
    void getSummary_withNoActivities_shouldReturnZeroValues() {
        when(repository.findByUserIdAndActivityDateBetween(any(UUID.class), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(repository.existsByUserIdAndActivityDate(any(UUID.class), any(LocalDate.class))).thenReturn(false);
        when(repository.findLongestDistance(userId)).thenReturn(null);

        StatsSummaryResponse response = service.getSummary(userId);

        assertNotNull(response);
        assertEquals(0.0, response.getKmWeek());
        assertEquals(0.0, response.getKmMonth());
        assertEquals(0, response.getActivitiesWeek());
        assertEquals(0, response.getStreak());
        assertEquals(0.0, response.getLongestDistance());
    }
}
