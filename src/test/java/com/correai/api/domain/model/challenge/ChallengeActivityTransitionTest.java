package com.correai.api.domain.model.challenge;

import com.correai.api.domain.model.activity.Activity;
import com.correai.api.domain.model.activity.ActivityType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ChallengeActivityTransitionTest {
    private final UUID user = UUID.randomUUID();
    private final Challenge challenge = new Challenge(UUID.randomUUID(), user, "June", "June activity",
            ChallengeAudience.FRIENDS, null, ChallengeMetric.DISTANCE_KM, 10,
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), "America/Sao_Paulo",
            UUID.randomUUID(), Instant.parse("2026-05-01T00:00:00Z"),
            Instant.parse("2026-05-01T00:00:00Z"));
    private final Instant joined = Instant.parse("2026-06-15T12:00:00Z");
    private final ChallengeParticipation participation = ChallengeParticipation.join(challenge, user, joined, List.of());

    private Activity activity(double distance, Instant start, Instant created) {
        return Activity.reconstructWithStartedAt(UUID.randomUUID(), user, ActivityType.RUN,
                LocalDate.of(2026, 6, 29), null, start, distance, 3600, 360, null, null, null, created);
    }

    @Test
    void evaluatesBothSidesOfMutationBeforeTheDeadline() {
        Instant now = Instant.parse("2026-06-29T12:00:00Z");
        var earned = ChallengeActivityTransition.evaluate(challenge, participation, List.of(),
                List.of(activity(10, now, now)), now, now.plusSeconds(1));
        assertEquals(10, earned.progress().current());
        assertEquals(now.plusSeconds(1), earned.progress().achievedAt());
        assertFalse(earned.finalized());
    }

    @Test
    void preservesAchievementBeforeDeletingAnActivity() {
        Instant now = Instant.parse("2026-06-29T12:00:00Z");
        var earned = ChallengeActivityTransition.evaluate(challenge, participation,
                List.of(activity(10, now.minusSeconds(1), joined)), List.of(), now, now.plusSeconds(1));
        assertEquals(now, earned.progress().achievedAt());
    }

    @Test
    void finalizesFromBeforeStateWhenMutationCrossesDeadline() {
        Instant end = challenge.endExclusive();
        var old = activity(4, joined, joined);
        var changed = old.update(ActivityType.RUN, 12, 3600, null, null, null);
        var result = ChallengeActivityTransition.evaluate(challenge, participation, List.of(old), List.of(changed),
                end.minusSeconds(1), end.plusSeconds(1));
        assertTrue(result.finalized());
        assertEquals(4, result.progress().current());
        assertNull(result.progress().achievedAt());
    }

    @Test
    void finalizesFutureActivityBeforeLateMutation() {
        Instant end = challenge.endExclusive();
        var old = activity(10, end.minusSeconds(1), joined);
        var result = ChallengeActivityTransition.evaluate(challenge, participation, List.of(old), List.of(),
                end, end.plusSeconds(1));
        assertTrue(result.finalized());
        assertEquals(10, result.progress().current());
        assertNotNull(result.progress().achievedAt());
    }

    @Test
    void rejectsClockMovingBackwards() {
        assertThrows(IllegalArgumentException.class, () -> ChallengeActivityTransition.evaluate(
                challenge, participation, List.of(), List.of(), joined.plusSeconds(1), joined));
    }
}
