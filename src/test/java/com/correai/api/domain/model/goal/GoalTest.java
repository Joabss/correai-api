package com.correai.api.domain.model.goal;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GoalTest {

    @Test
    void create_shouldBeActiveAndWithoutId() {
        Goal goal = Goal.create(UUID.randomUUID(), GoalType.DISTANCE_KM, GoalPeriod.WEEKLY, 30.0);

        assertNull(goal.id());
        assertTrue(goal.active());
        assertEquals(30.0, goal.target());
    }

    @Test
    void create_withNonPositiveTarget_shouldThrow() {
        UUID userId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> Goal.create(userId, GoalType.ACTIVITIES, GoalPeriod.WEEKLY, 0));
        assertThrows(IllegalArgumentException.class, () -> Goal.create(userId, GoalType.ACTIVITIES, GoalPeriod.WEEKLY, -1));
        assertThrows(IllegalArgumentException.class, () -> Goal.create(userId, GoalType.ACTIVITIES, GoalPeriod.WEEKLY, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> Goal.create(userId, GoalType.ACTIVITIES, GoalPeriod.WEEKLY, Double.POSITIVE_INFINITY));
    }

    @Test
    void awardedGoalIsImmutableAndProtected() {
        Goal goal = Goal.create(UUID.randomUUID(), GoalType.DISTANCE_KM, GoalPeriod.WEEKLY, 5);
        var at = java.time.Instant.parse("2026-09-30T12:00:00Z");
        Goal awarded = goal.achieve(at, LocalDate.of(2026, 9, 28), 5);
        assertFalse(goal.hasBadge());
        assertTrue(awarded.hasBadge());
        assertSame(awarded, awarded.achieve(at.plusSeconds(5), LocalDate.of(2026, 10, 5), 10));
        assertThrows(GoalHasBadgeException.class, awarded::deactivate);
        assertThrows(IllegalArgumentException.class, () -> goal.achieve(at, LocalDate.now(), 4.999));
    }

    @Test
    void deactivate_shouldReturnInactiveCopy() {
        Goal goal = Goal.create(UUID.randomUUID(), GoalType.ACTIVITIES, GoalPeriod.MONTHLY, 12);

        Goal inactive = goal.deactivate();

        assertFalse(inactive.active());
        assertTrue(goal.active());
    }

    @Test
    void sameKindAs_shouldCompareTypeAndPeriod() {
        UUID userId = UUID.randomUUID();
        Goal weeklyKm = Goal.create(userId, GoalType.DISTANCE_KM, GoalPeriod.WEEKLY, 20);

        assertTrue(weeklyKm.sameKindAs(Goal.create(userId, GoalType.DISTANCE_KM, GoalPeriod.WEEKLY, 30)));
        assertFalse(weeklyKm.sameKindAs(Goal.create(userId, GoalType.DISTANCE_KM, GoalPeriod.MONTHLY, 20)));
        assertFalse(weeklyKm.sameKindAs(Goal.create(userId, GoalType.ACTIVITIES, GoalPeriod.WEEKLY, 20)));
    }

    @Test
    void period_shouldComputeBounds() {
        LocalDate wednesday = LocalDate.of(2026, 9, 30);

        assertEquals(LocalDate.of(2026, 9, 28), GoalPeriod.WEEKLY.startOf(wednesday));
        assertEquals(LocalDate.of(2026, 10, 4), GoalPeriod.WEEKLY.endOf(wednesday));
        assertEquals(LocalDate.of(2026, 9, 1), GoalPeriod.MONTHLY.startOf(wednesday));
        assertEquals(LocalDate.of(2026, 9, 30), GoalPeriod.MONTHLY.endOf(wednesday));
    }
}
