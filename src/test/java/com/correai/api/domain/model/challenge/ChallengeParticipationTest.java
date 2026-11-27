package com.correai.api.domain.model.challenge;

import com.correai.api.domain.model.activity.Activity;
import com.correai.api.domain.model.activity.ActivityType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ChallengeParticipationTest {
    private final UUID creator = UUID.randomUUID();
    private final UUID participant = UUID.randomUUID();
    private final Instant publishedAt = Instant.parse("2026-05-01T00:00:00Z");
    private final Challenge challenge = new Challenge(UUID.randomUUID(), creator, "June", "June activity",
            ChallengeAudience.FRIENDS, null, ChallengeMetric.DISTANCE_KM, 10,
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), "America/Sao_Paulo",
            UUID.randomUUID(), publishedAt.minusSeconds(1), publishedAt);

    @Test
    void joinsOnlyDuringThePublishedChallengePeriodInItsTimeZone() {
        Instant start = challenge.startAt();
        Instant end = challenge.endExclusive();
        assertThrows(ChallengeConflictException.class,
                () -> ChallengeParticipation.join(challenge, participant, start.minusNanos(1), List.of()));
        assertEquals(start, ChallengeParticipation.join(challenge, participant, start, List.of()).joinedAt());
        assertEquals(end.minusNanos(1),
                ChallengeParticipation.join(challenge, participant, end.minusNanos(1), List.of()).joinedAt());
        assertThrows(ChallengeConflictException.class,
                () -> ChallengeParticipation.join(challenge, participant, end, List.of()));
    }

    @Test
    void joiningCountsActivitiesFromBeforeJoiningAndGrantsImmediatelyAtThreshold() {
        Instant joinedAt = Instant.parse("2026-06-20T12:00:00Z");
        Activity activity = Activity.reconstructWithStartedAt(UUID.randomUUID(), participant, ActivityType.RUN,
                LocalDate.of(2026, 6, 5), null, Instant.parse("2026-06-05T12:00:00Z"),
                10.0, 3600, 360, null, null, null, Instant.parse("2026-06-05T13:00:00Z"));
        ChallengeParticipation joined = ChallengeParticipation.join(challenge, participant, joinedAt, List.of(activity));
        assertEquals(10, joined.progress().current());
        assertEquals(joinedAt, joined.progress().achievedAt());
        assertEquals(joinedAt, joined.joinedAt());
        assertEquals(challenge.id(), joined.challengeId());
    }

    @Test
    void refusesDraftsOrParticipationBeforePublication() {
        Challenge draft = new Challenge(challenge.id(), creator, challenge.name(), challenge.description(),
                challenge.audience(), null, challenge.metric(), challenge.target(), challenge.startDate(),
                challenge.endDate(), challenge.timeZone(), challenge.badgeTemplateId(), challenge.createdAt(), null);
        assertThrows(ChallengeConflictException.class,
                () -> ChallengeParticipation.join(draft, participant, challenge.startAt(), List.of()));
        Challenge publishedLater = new Challenge(challenge.id(), creator, challenge.name(), challenge.description(),
                challenge.audience(), null, challenge.metric(), challenge.target(), challenge.startDate(),
                challenge.endDate(), challenge.timeZone(), challenge.badgeTemplateId(), challenge.createdAt(),
                challenge.startAt().plusSeconds(10));
        assertThrows(ChallengeConflictException.class,
                () -> ChallengeParticipation.join(publishedLater, participant, challenge.startAt(), List.of()));
    }

    @Test
    void refreshRefusesAnotherChallengeOrTimeBeforeJoining() {
        Instant joinedAt = challenge.startAt();
        ChallengeParticipation participation = ChallengeParticipation.join(challenge, participant, joinedAt, List.of());
        Challenge other = new Challenge(UUID.randomUUID(), creator, challenge.name(), challenge.description(),
                challenge.audience(), null, challenge.metric(), challenge.target(), challenge.startDate(),
                challenge.endDate(), challenge.timeZone(), challenge.badgeTemplateId(), challenge.createdAt(), publishedAt);
        assertThrows(IllegalArgumentException.class, () -> participation.refresh(other, List.of(), joinedAt));
        assertThrows(IllegalArgumentException.class,
                () -> participation.refresh(challenge, List.of(), joinedAt.minusSeconds(1)));
    }

    @Test
    void finalizesFutureDatedActivityAtEndBeforeItCanBeDeleted() {
        Instant joinedAt = challenge.startAt();
        Instant futureStart = Instant.parse("2026-06-29T12:00:00Z");
        Activity activity = Activity.reconstructWithStartedAt(UUID.randomUUID(), participant, ActivityType.RUN,
                LocalDate.of(2026, 6, 29), null, futureStart, 10.0, 3600, 360,
                null, null, null, joinedAt);
        ChallengeParticipation joined = ChallengeParticipation.join(challenge, participant, joinedAt, List.of(activity));
        assertEquals(0, joined.progress().current());
        var ended = joined.refresh(challenge, List.of(activity), challenge.endExclusive());
        assertTrue(ended.finalized());
        assertEquals(10, ended.progress().current());
        assertEquals(challenge.endExclusive(), ended.progress().achievedAt());
        assertEquals(ended, ended.refresh(challenge, List.of(), challenge.endExclusive().plusSeconds(10)));
    }

    @Test
    void finalizedUnawardedParticipationCannotCountLateActivity() {
        Instant joinedAt = challenge.startAt();
        var joined = ChallengeParticipation.join(challenge, participant, joinedAt, List.of());
        var ended = joined.refresh(challenge, List.of(), challenge.endExclusive());
        Activity late = Activity.reconstructWithStartedAt(UUID.randomUUID(), participant, ActivityType.RUN,
                LocalDate.of(2026, 6, 5), null, Instant.parse("2026-06-05T12:00:00Z"),
                10.0, 3600, 360, null, null, null, challenge.endExclusive().plusSeconds(1));
        assertTrue(ended.finalized());
        assertNull(ended.refresh(challenge, List.of(late), challenge.endExclusive().plusSeconds(2))
                .progress().achievedAt());
    }
}
