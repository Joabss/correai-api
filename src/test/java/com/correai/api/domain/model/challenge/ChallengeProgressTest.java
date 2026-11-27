package com.correai.api.domain.model.challenge;

import com.correai.api.domain.model.activity.Activity;
import com.correai.api.domain.model.activity.ActivityType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ChallengeProgressTest {
    private final UUID user = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-06-15T15:00:00Z");

    private Challenge challenge(ChallengeMetric metric, double target) {
        return new Challenge(UUID.randomUUID(), user, "Challenge", "Description",
                ChallengeAudience.FRIENDS, null, metric, target,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), "America/Sao_Paulo",
                UUID.randomUUID(), Instant.parse("2026-05-01T00:00:00Z"), now.minusSeconds(1));
    }

    private Activity activity(Instant startedAt, Instant createdAt, double distance) {
        return Activity.reconstructWithStartedAt(UUID.randomUUID(), user, ActivityType.RUN,
                LocalDate.of(2026, 6, 15), null, startedAt, distance, 1000, 100,
                null, null, null, createdAt);
    }

    @Test
    void includesEarlierActivitiesAndUsesExclusiveZoneBoundaries() {
        Challenge c = challenge(ChallengeMetric.DISTANCE_KM, 20);
        var start = c.startAt();
        var result = ChallengeProgress.empty().refresh(c, user, List.of(
                activity(start.minusNanos(1), now.minusSeconds(1), 100),
                activity(start, start.minusNanos(1), 100),
                activity(start, now.minusSeconds(1), 4),
                activity(now, now.minusSeconds(1), 5),
                activity(now.plusNanos(1), now.minusSeconds(1), 100),
                activity(c.endExclusive(), now.minusSeconds(1), 100),
                activity(now, c.endExclusive(), 100)), now);
        assertEquals(9, result.current());
        assertNull(result.achievedAt());
    }

    @Test
    void grantsAtThresholdAndPreservesAwardAfterDeletionAndEnd() {
        Challenge c = challenge(ChallengeMetric.ACTIVITIES, 2);
        var first = activity(now.minusSeconds(3), now.minusSeconds(2), 1);
        var second = activity(now.minusSeconds(2), now.minusSeconds(1), 2);
        var awarded = ChallengeProgress.empty().refresh(c, user, List.of(first, second), now);
        assertEquals(2, awarded.current());
        assertEquals(now, awarded.achievedAt());
        assertSame(awarded, awarded.refresh(c, user, List.of(), now.plusSeconds(5)));
        assertSame(awarded, awarded.refresh(c, user, List.of(), c.endExclusive()));
    }

    @Test
    void freezesUnawardedSnapshotAtEndAndIgnoresLaterChanges() {
        Challenge c = challenge(ChallengeMetric.DISTANCE_KM, 10);
        var before = ChallengeProgress.empty().refresh(c, user, List.of(
                activity(now, now, 3)), now);
        assertEquals(3, before.current());
        assertSame(before, before.refresh(c, user, List.of(activity(now, now, 100)), c.endExclusive()));
        assertSame(before, before.refresh(c, user, List.of(), c.endExclusive().plusSeconds(1)));
    }

    @Test
    void recomputesUnawardedProgressWhenActivitiesChangeBeforeEnd() {
        Challenge c = challenge(ChallengeMetric.DISTANCE_KM, 10);
        var first = activity(now, now, 3);
        var progress = ChallengeProgress.empty().refresh(c, user, List.of(first), now);
        assertEquals(0, progress.refresh(c, user, List.of(), now.plusSeconds(1)).current());
        var updated = first.update(ActivityType.RUN, 6, 1000, null, null, null);
        assertEquals(6, progress.refresh(c, user, List.of(updated), now.plusSeconds(1)).current());
    }

    @Test
    void supportsDateOnlyActivitiesAndRejectsFutureDates() {
        Challenge c = challenge(ChallengeMetric.ACTIVITIES, 5);
        Activity legacy = Activity.reconstruct(UUID.randomUUID(), user, ActivityType.WALK,
                LocalDate.of(2026, 6, 14), 3.0, 1000, 333, null, null, null, now.minusSeconds(100));
        Activity future = Activity.reconstruct(UUID.randomUUID(), user, ActivityType.WALK,
                LocalDate.of(2026, 6, 16), 3.0, 1000, 333, null, null, null, now.minusSeconds(100));
        assertEquals(1, ChallengeProgress.empty().refresh(c, user, List.of(legacy, future), now).current());
    }

    @Test
    void rejectsUnpublishedChallengeAndInvalidSnapshot() {
        Challenge c = challenge(ChallengeMetric.ACTIVITIES, 1);
        Challenge draft = new Challenge(c.id(), c.creatorId(), c.name(), c.description(), c.audience(),
                null, c.metric(), c.target(), c.startDate(), c.endDate(), c.timeZone(),
                c.badgeTemplateId(), c.createdAt(), null);
        assertThrows(IllegalArgumentException.class, () -> ChallengeProgress.empty().refresh(draft, user, List.of(), now));
        assertThrows(IllegalArgumentException.class, () -> new ChallengeProgress(Double.NaN, null));
    }

    @Test
    void refusesToCalculateProgressFromAnotherUserActivities() {
        Challenge c = challenge(ChallengeMetric.ACTIVITIES, 1);
        assertThrows(IllegalArgumentException.class,
                () -> ChallengeProgress.empty().refresh(c, UUID.randomUUID(),
                        List.of(activity(now, now, 1)), now));
    }

    @Test
    void excludesActivitiesWithoutKnownCreationTime() {
        var unpersisted = Activity.create(user, ActivityType.RUN, 1, 100, null, null, null,
                LocalDate.of(2026, 6, 14));
        var result = ChallengeProgress.empty().refresh(
                challenge(ChallengeMetric.DISTANCE_KM, 1), user, List.of(unpersisted), now);
        assertEquals(0, result.current());
        assertNull(result.achievedAt());
    }
}
