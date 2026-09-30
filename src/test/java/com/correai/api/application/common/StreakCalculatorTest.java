package com.correai.api.application.common;

import com.correai.api.domain.port.out.activity.ActivityRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StreakCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 30);

    @Mock
    private ActivityRepositoryPort repository;

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-30T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void currentStreak_shouldCountConsecutiveDaysEndingToday() {
        UUID userId = UUID.randomUUID();
        when(repository.existsByUserIdAndActivityDate(userId, TODAY)).thenReturn(true);
        when(repository.existsByUserIdAndActivityDate(userId, TODAY.minusDays(1))).thenReturn(true);
        when(repository.existsByUserIdAndActivityDate(userId, TODAY.minusDays(2))).thenReturn(false);

        assertEquals(2, new StreakCalculator(repository, clock).currentStreak(userId));
    }

    @Test
    void currentStreak_withoutActivityToday_shouldBeZero() {
        UUID userId = UUID.randomUUID();
        when(repository.existsByUserIdAndActivityDate(userId, TODAY)).thenReturn(false);

        assertEquals(0, new StreakCalculator(repository, clock).currentStreak(userId));
    }
}
