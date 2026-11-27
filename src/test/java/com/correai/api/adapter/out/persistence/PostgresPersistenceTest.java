package com.correai.api.adapter.out.persistence;

import com.correai.api.domain.model.activity.Activity;
import com.correai.api.domain.model.activity.ActivityType;
import com.correai.api.domain.model.activity.PerceivedEffort;
import com.correai.api.domain.model.activity.TrainingType;
import com.correai.api.domain.model.goal.Goal;
import com.correai.api.domain.model.goal.GoalPeriod;
import com.correai.api.domain.model.goal.GoalType;
import com.correai.api.domain.model.pagination.PageQuery;
import com.correai.api.domain.model.pagination.PageResult;
import com.correai.api.domain.model.user.User;
import com.correai.api.domain.model.user.ConcurrentProfileUpdateException;
import com.correai.api.domain.port.out.activity.ActivityRepositoryPort;
import com.correai.api.domain.port.out.goal.GoalRepositoryPort;
import com.correai.api.domain.port.in.group.GroupsUseCase;
import com.correai.api.domain.port.in.admin.AdminUseCase;
import com.correai.api.domain.port.in.badge.BadgeTemplatesUseCase;
import com.correai.api.domain.model.badge.BadgeIcon;
import com.correai.api.domain.model.challenge.ChallengeAudience;
import com.correai.api.domain.model.challenge.ChallengeMetric;
import com.correai.api.domain.port.in.challenge.ChallengeDraftCommand;
import com.correai.api.domain.port.in.challenge.ChallengeDraftsUseCase;
import com.correai.api.domain.port.in.challenge.ChallengesUseCase;
import com.correai.api.domain.port.in.activity.CreateActivityUseCase;
import com.correai.api.domain.port.in.activity.CreateActivityCommand;
import com.correai.api.domain.port.in.activity.DeleteActivityUseCase;
import com.correai.api.domain.port.out.challenge.ChallengeParticipationRepositoryPort;
import com.correai.api.domain.port.out.challenge.ChallengeRepositoryPort;
import com.correai.api.domain.model.challenge.Challenge;
import com.correai.api.domain.model.challenge.ChallengeConflictException;
import com.correai.api.domain.model.challenge.ChallengeParticipation;
import com.correai.api.domain.model.challenge.ChallengeProgress;
import com.correai.api.domain.model.admin.AdminConflictException;
import com.correai.api.domain.port.in.friendship.FriendshipsUseCase;
import com.correai.api.domain.model.friendship.FriendshipState;
import com.correai.api.domain.port.out.user.UserRepositoryPort;
import com.correai.api.domain.port.out.media.MediaRepositoryPort;
import com.correai.api.domain.model.media.MediaContent;
import com.correai.api.domain.model.media.MediaKind;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs the Flyway migrations against a real PostgreSQL and validates them against the JPA mapping
 * ({@code ddl-auto: validate}). Skipped when Docker is not available.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.create-schemas=true",
        "spring.flyway.schemas=correai",
        "spring.flyway.default-schema=correai",
        "spring.jpa.properties.hibernate.default_schema=correai",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect"
})
class PostgresPersistenceTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

    @Autowired
    private UserRepositoryPort userRepository;

    @Autowired
    private ActivityRepositoryPort activityRepository;

    @Autowired
    private GoalRepositoryPort goalRepository;

    @Autowired
    private MediaRepositoryPort mediaRepository;

    @Autowired
    private GroupsUseCase groups;

    @Autowired
    private FriendshipsUseCase friendships;

    @Autowired
    private AdminUseCase admins;

    @Autowired
    private BadgeTemplatesUseCase badgeTemplates;

    @Autowired
    private ChallengeDraftsUseCase challengeDrafts;

    @Autowired
    private ChallengeParticipationRepositoryPort challengeParticipations;

    @Autowired
    private ChallengeRepositoryPort challengeRepository;

    @Autowired
    private CreateActivityUseCase createActivity;

    @Autowired
    private DeleteActivityUseCase deleteActivity;

    @Autowired
    private ChallengesUseCase challenges;

    @Test
    void concurrentJoinAndActivityAlwaysCountTheActivity() throws Exception {
        UUID owner = userRepository.save(User.anonymous()).id();
        UUID participant = userRepository.save(User.anonymous()).id();
        UUID badge = UUID.randomUUID();
        Instant now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        LocalDate today = LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo"));
        jdbc.update("insert into correai.badge_templates (id, name, description, icon, color, created_by, created_at) "
                        + "values (?, ?, ?, ?, ?, ?, ?)", badge, "Steps", "Walk", "FOOTPRINT", "#12AB34",
                owner, java.sql.Timestamp.from(now));
        friendships.request(owner, participant);
        friendships.accept(participant, owner);
        var draft = challengeDrafts.create(owner, new ChallengeDraftCommand("Walk", "Walk outside",
                ChallengeAudience.FRIENDS, null, ChallengeMetric.DISTANCE_KM, 5,
                today.minusDays(1), today.plusDays(1), "America/Sao_Paulo", badge));
        challengeDrafts.publish(owner, draft.id());
        var ready = new java.util.concurrent.CountDownLatch(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var joining = executor.submit(() -> {
                ready.countDown();
                start.await();
                return challenges.join(participant, draft.id());
            });
            var activity = executor.submit(() -> {
                ready.countDown();
                start.await();
                return createActivity.create(new CreateActivityCommand(participant, ActivityType.WALK,
                        5, 1800, null, null, null, null));
            });
            assertTrue(ready.await(5, java.util.concurrent.TimeUnit.SECONDS));
            start.countDown();
            joining.get(15, java.util.concurrent.TimeUnit.SECONDS);
            activity.get(15, java.util.concurrent.TimeUnit.SECONDS);
        }
        var progress = challengeParticipations.find(draft.id(), participant).orElseThrow().progress();
        assertEquals(5, progress.current());
        assertNotNull(progress.achievedAt());
    }

    @Test
    void concurrentBlockAndJoinCannotLeaveChallengeVisible() throws Exception {
        UUID owner = userRepository.save(User.anonymous()).id();
        UUID participant = userRepository.save(User.anonymous()).id();
        UUID badge = UUID.randomUUID();
        Instant now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        LocalDate today = LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo"));
        jdbc.update("insert into correai.badge_templates (id, name, description, icon, color, created_by, created_at) "
                        + "values (?, ?, ?, ?, ?, ?, ?)", badge, "Steps", "Walk", "FOOTPRINT", "#12AB34",
                owner, java.sql.Timestamp.from(now));
        friendships.request(owner, participant);
        friendships.accept(participant, owner);
        var draft = challengeDrafts.create(owner, new ChallengeDraftCommand("Walk", "Walk outside",
                ChallengeAudience.FRIENDS, null, ChallengeMetric.DISTANCE_KM, 5,
                today.minusDays(1), today.plusDays(1), "America/Sao_Paulo", badge));
        challengeDrafts.publish(owner, draft.id());
        var ready = new java.util.concurrent.CountDownLatch(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var joining = executor.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    challenges.join(participant, draft.id());
                    return true;
                } catch (com.correai.api.domain.model.challenge.ChallengeNotFoundException expected) {
                    return false;
                }
            });
            var blocking = executor.submit(() -> {
                ready.countDown();
                start.await();
                friendships.block(owner, participant);
                return null;
            });
            assertTrue(ready.await(5, java.util.concurrent.TimeUnit.SECONDS));
            start.countDown();
            joining.get(15, java.util.concurrent.TimeUnit.SECONDS);
            blocking.get(15, java.util.concurrent.TimeUnit.SECONDS);
        }
        assertTrue(challengeRepository.findVisible(participant, draft.id()).isEmpty());
        assertThrows(com.correai.api.domain.model.challenge.ChallengeNotFoundException.class,
                () -> challenges.get(participant, draft.id()));
    }

    @Test
    void activityCreationEvaluatesAndPreservesChallengeAward() {
        UUID user = userRepository.save(User.anonymous()).id();
        UUID badge = UUID.randomUUID();
        Instant now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        LocalDate today = LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo"));
        jdbc.update("insert into correai.badge_templates (id, name, description, icon, color, created_by, created_at) "
                        + "values (?, ?, ?, ?, ?, ?, ?)", badge, "Steps", "Walk", "FOOTPRINT", "#12AB34",
                user, java.sql.Timestamp.from(now.minusSeconds(20)));
        Challenge challenge = challengeDrafts.create(user, new ChallengeDraftCommand("Walk", "Walk outside",
                ChallengeAudience.FRIENDS, null, ChallengeMetric.DISTANCE_KM, 5,
                today.minusDays(1), today.plusDays(1), "America/Sao_Paulo", badge));
        jdbc.update("update correai.shared_challenges set published_at = ? where id = ?",
                java.sql.Timestamp.from(now.minusSeconds(10)), challenge.id());
        challengeParticipations.create(new ChallengeParticipation(challenge.id(), user, now.minusSeconds(1),
                ChallengeProgress.empty(), false));
        var activity = createActivity.create(new CreateActivityCommand(user, ActivityType.WALK,
                5, 1800, null, null, null, null)).activity();
        var awarded = challengeParticipations.find(challenge.id(), user).orElseThrow();
        assertEquals(5, awarded.progress().current());
        assertNotNull(awarded.progress().achievedAt());
        deleteActivity.delete(user, activity.id());
        assertEquals(awarded, challengeParticipations.find(challenge.id(), user).orElseThrow());
    }

    @Test
    void lateActivityDeletionFinalizesPriorProgressBeforeRemovingActivity() {
        UUID user = userRepository.save(User.anonymous()).id();
        UUID badge = UUID.randomUUID();
        var zone = java.time.ZoneId.of("America/Sao_Paulo");
        LocalDate day = LocalDate.now(zone).minusDays(1);
        Instant published = day.atTime(7, 0).atZone(zone).toInstant();
        Instant joined = day.atTime(8, 0).atZone(zone).toInstant();
        Instant started = day.atTime(9, 0).atZone(zone).toInstant();
        Instant created = day.atTime(10, 0).atZone(zone).toInstant();
        jdbc.update("insert into correai.badge_templates (id, name, description, icon, color, created_by, created_at) "
                        + "values (?, ?, ?, ?, ?, ?, ?)", badge, "Steps", "Walk", "FOOTPRINT", "#12AB34",
                user, java.sql.Timestamp.from(published));
        Challenge challenge = challengeDrafts.create(user, new ChallengeDraftCommand("Walk", "Walk outside",
                ChallengeAudience.FRIENDS, null, ChallengeMetric.DISTANCE_KM, 5,
                day, day, "America/Sao_Paulo", badge));
        jdbc.update("update correai.shared_challenges set published_at = ? where id = ?",
                java.sql.Timestamp.from(published), challenge.id());
        challengeParticipations.create(new ChallengeParticipation(challenge.id(), user, joined,
                ChallengeProgress.empty(), false));
        Activity activity = activityRepository.save(Activity.createWithStartedAt(user, ActivityType.WALK,
                5, 1800, null, null, null, started.atOffset(ZoneOffset.UTC)));
        jdbc.update("update correai.activities set created_at = ? where id = ?",
                java.sql.Timestamp.from(created), activity.id());
        deleteActivity.delete(user, activity.id());
        var result = challengeParticipations.find(challenge.id(), user).orElseThrow();
        assertTrue(result.finalized());
        assertEquals(5, result.progress().current());
        assertNotNull(result.progress().achievedAt());
        assertTrue(result.progress().achievedAt().isAfter(challenge.endExclusive()));
        assertTrue(activityRepository.findByIdAndUserId(activity.id(), user).isEmpty());
    }

    @Test
    void activityRegisteredAfterChallengeEndDoesNotEarnBadge() {
        UUID user = userRepository.save(User.anonymous()).id();
        UUID badge = UUID.randomUUID();
        var zone = java.time.ZoneId.of("America/Sao_Paulo");
        LocalDate day = LocalDate.now(zone).minusDays(1);
        Instant published = day.atTime(7, 0).atZone(zone).toInstant();
        Instant joined = day.atTime(8, 0).atZone(zone).toInstant();
        Instant started = day.atTime(9, 0).atZone(zone).toInstant();
        jdbc.update("insert into correai.badge_templates (id, name, description, icon, color, created_by, created_at) "
                        + "values (?, ?, ?, ?, ?, ?, ?)", badge, "Steps", "Walk", "FOOTPRINT", "#12AB34",
                user, java.sql.Timestamp.from(published));
        Challenge challenge = challengeDrafts.create(user, new ChallengeDraftCommand("Walk", "Walk outside",
                ChallengeAudience.FRIENDS, null, ChallengeMetric.DISTANCE_KM, 5,
                day, day, "America/Sao_Paulo", badge));
        jdbc.update("update correai.shared_challenges set published_at = ? where id = ?",
                java.sql.Timestamp.from(published), challenge.id());
        challengeParticipations.create(new ChallengeParticipation(challenge.id(), user, joined,
                ChallengeProgress.empty(), false));
        createActivity.create(new CreateActivityCommand(user, ActivityType.WALK,
                5, 1800, null, null, null, started.atOffset(ZoneOffset.UTC)));
        var result = challengeParticipations.find(challenge.id(), user).orElseThrow();
        assertTrue(result.finalized());
        assertEquals(0, result.progress().current());
        assertNull(result.progress().achievedAt());
        assertEquals(0, challenges.listBadges(user, new PageQuery(0, 20)).totalElements());
    }

    @Test
    void preventsConcurrentDraftEditsFromOverwritingEachOther() throws Exception {
        UUID user = userRepository.save(User.anonymous()).id();
        UUID badge = UUID.randomUUID();
        Instant at = Instant.parse("2026-06-15T12:00:00Z");
        jdbc.update("insert into correai.badge_templates (id, name, description, icon, color, created_by, created_at) "
                        + "values (?, ?, ?, ?, ?, ?, ?)", badge, "Steps", "Walk", "FOOTPRINT", "#12AB34",
                user, java.sql.Timestamp.from(at));
        Challenge original = challengeDrafts.create(user, new ChallengeDraftCommand("Walk", "Walk outside",
                ChallengeAudience.FRIENDS, null, ChallengeMetric.DISTANCE_KM, 20,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), "America/Sao_Paulo", badge));
        Challenge first = original.revise("First", original.description(), original.audience(), null,
                original.metric(), original.target(), original.startDate(), original.endDate(),
                original.timeZone(), badge);
        Challenge second = original.revise("Second", original.description(), original.audience(), null,
                original.metric(), original.target(), original.startDate(), original.endDate(),
                original.timeZone(), badge);
        var ready = new java.util.concurrent.CountDownLatch(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var one = executor.submit(() -> tryDraftEdit(original, first, ready, start));
            var two = executor.submit(() -> tryDraftEdit(original, second, ready, start));
            assertTrue(ready.await(5, java.util.concurrent.TimeUnit.SECONDS));
            start.countDown();
            assertNotEquals(one.get(10, java.util.concurrent.TimeUnit.SECONDS),
                    two.get(10, java.util.concurrent.TimeUnit.SECONDS));
        }
        Challenge stored = challengeDrafts.getOwn(user, original.id());
        assertTrue(stored.equals(first) || stored.equals(second));
    }

    @Test
    void publishingAndEditingConcurrentlyCannotPublishStaleState() throws Exception {
        UUID user = userRepository.save(User.anonymous()).id();
        UUID badge = UUID.randomUUID();
        Instant now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        LocalDate today = LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo"));
        jdbc.update("insert into correai.badge_templates (id, name, description, icon, color, created_by, created_at) "
                        + "values (?, ?, ?, ?, ?, ?, ?)", badge, "Steps", "Walk", "FOOTPRINT", "#12AB34",
                user, java.sql.Timestamp.from(now));
        var original = new ChallengeDraftCommand("Original", "Walk", ChallengeAudience.FRIENDS, null,
                ChallengeMetric.DISTANCE_KM, 20, today.minusDays(1), today.plusDays(1), "America/Sao_Paulo", badge);
        var revised = new ChallengeDraftCommand("Revised", "Walk", ChallengeAudience.FRIENDS, null,
                ChallengeMetric.DISTANCE_KM, 20, today.minusDays(1), today.plusDays(1), "America/Sao_Paulo", badge);
        var draft = challengeDrafts.create(user, original);
        var ready = new java.util.concurrent.CountDownLatch(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var edit = executor.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    challengeDrafts.edit(user, draft.id(), revised);
                    return true;
                } catch (ChallengeConflictException expected) {
                    return false;
                }
            });
            var publish = executor.submit(() -> {
                ready.countDown();
                start.await();
                return challengeDrafts.publish(user, draft.id());
            });
            assertTrue(ready.await(5, java.util.concurrent.TimeUnit.SECONDS));
            start.countDown();
            boolean edited = edit.get(15, java.util.concurrent.TimeUnit.SECONDS);
            Challenge published = publish.get(15, java.util.concurrent.TimeUnit.SECONDS);
            assertEquals(edited ? "Revised" : "Original", published.name());
            assertEquals(published, challengeDrafts.getOwn(user, draft.id()));
        }
    }

    private boolean tryDraftEdit(Challenge original, Challenge revised,
                                 java.util.concurrent.CountDownLatch ready,
                                 java.util.concurrent.CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            challengeRepository.saveDraft(original, revised);
            return true;
        } catch (ChallengeConflictException expected) {
            return false;
        }
    }

    @Test
    void persistsParticipationAndProtectsAwardedProgress() {
        UUID user = userRepository.save(User.anonymous()).id();
        UUID badge = UUID.randomUUID();
        Instant at = Instant.parse("2026-06-15T12:00:00Z");
        jdbc.update("insert into correai.badge_templates (id, name, description, icon, color, created_by, created_at) "
                        + "values (?, ?, ?, ?, ?, ?, ?)", badge, "Steps", "Walk", "FOOTPRINT", "#12AB34",
                user, java.sql.Timestamp.from(at));
        var challenge = challengeDrafts.create(user, new ChallengeDraftCommand("Walk", "Walk outside",
                ChallengeAudience.FRIENDS, null, ChallengeMetric.DISTANCE_KM, 20,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), "America/Sao_Paulo", badge));
        var initial = new ChallengeParticipation(challenge.id(), user, at, new ChallengeProgress(5, null), false);
        assertThrows(com.correai.api.domain.model.challenge.ChallengeConflictException.class,
                () -> challengeParticipations.create(initial));
        jdbc.update("update correai.shared_challenges set published_at = ? where id = ?",
                java.sql.Timestamp.from(at), challenge.id());
        assertEquals(initial, challengeParticipations.create(initial));
        assertEquals(initial, challengeParticipations.create(initial));
        assertEquals(1, challengeParticipations.findPendingByUserId(user).size());
        var earned = new ChallengeParticipation(challenge.id(), user, at, new ChallengeProgress(20, at), false);
        assertEquals(earned, challengeParticipations.update(initial, earned));
        assertTrue(challengeParticipations.findPendingByUserId(user).isEmpty());
        assertThrows(ChallengeConflictException.class, () -> challengeParticipations.update(initial, initial));
        assertEquals(earned, challengeParticipations.find(challenge.id(), user).orElseThrow());
        assertEquals(1, challengeParticipations.findByUserId(user).size());
        assertTrue(challengeParticipations.find(challenge.id(), UUID.randomUUID()).isEmpty());
    }

    @Test
    void persistsFinalizedParticipationWithRecognitionAfterDeadline() {
        UUID user = userRepository.save(User.anonymous()).id();
        UUID badge = UUID.randomUUID();
        Instant at = Instant.parse("2026-06-15T12:00:00Z");
        jdbc.update("insert into correai.badge_templates (id, name, description, icon, color, created_by, created_at) "
                        + "values (?, ?, ?, ?, ?, ?, ?)", badge, "Steps", "Walk", "FOOTPRINT", "#12AB34",
                user, java.sql.Timestamp.from(at));
        Challenge challenge = challengeDrafts.create(user, new ChallengeDraftCommand("Walk", "Walk outside",
                ChallengeAudience.FRIENDS, null, ChallengeMetric.DISTANCE_KM, 20,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), "America/Sao_Paulo", badge));
        jdbc.update("update correai.shared_challenges set published_at = ? where id = ?",
                java.sql.Timestamp.from(at), challenge.id());
        var initial = challengeParticipations.create(new ChallengeParticipation(challenge.id(), user, at,
                new ChallengeProgress(5, null), false));
        var finalized = new ChallengeParticipation(challenge.id(), user, at,
                new ChallengeProgress(20, challenge.endExclusive().plusSeconds(5)), true);
        var saved = challengeParticipations.update(initial, finalized);
        assertTrue(saved.finalized());
        assertEquals(challenge.endExclusive().plusSeconds(5), saved.progress().achievedAt());
        assertEquals(saved, challengeParticipations.find(challenge.id(), user).orElseThrow());
        assertThrows(ChallengeConflictException.class, () -> challengeParticipations.update(saved, initial));
    }

    @Test
    void preventsConcurrentParticipationUpdatesFromOverwritingEachOther() throws Exception {
        UUID user = userRepository.save(User.anonymous()).id();
        UUID badge = UUID.randomUUID();
        Instant at = Instant.parse("2026-06-15T12:00:00Z");
        jdbc.update("insert into correai.badge_templates (id, name, description, icon, color, created_by, created_at) "
                        + "values (?, ?, ?, ?, ?, ?, ?)", badge, "Steps", "Walk", "FOOTPRINT", "#12AB34",
                user, java.sql.Timestamp.from(at));
        Challenge challenge = challengeDrafts.create(user, new ChallengeDraftCommand("Walk", "Walk outside",
                ChallengeAudience.FRIENDS, null, ChallengeMetric.DISTANCE_KM, 20,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), "America/Sao_Paulo", badge));
        jdbc.update("update correai.shared_challenges set published_at = ? where id = ?",
                java.sql.Timestamp.from(at), challenge.id());
        var original = challengeParticipations.create(new ChallengeParticipation(challenge.id(), user, at,
                new ChallengeProgress(5, null), false));
        var first = new ChallengeParticipation(challenge.id(), user, at, new ChallengeProgress(6, null), false);
        var second = new ChallengeParticipation(challenge.id(), user, at, new ChallengeProgress(7, null), false);
        var ready = new java.util.concurrent.CountDownLatch(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var one = executor.submit(() -> tryParticipationUpdate(original, first, ready, start));
            var two = executor.submit(() -> tryParticipationUpdate(original, second, ready, start));
            assertTrue(ready.await(5, java.util.concurrent.TimeUnit.SECONDS));
            start.countDown();
            assertNotEquals(one.get(10, java.util.concurrent.TimeUnit.SECONDS),
                    two.get(10, java.util.concurrent.TimeUnit.SECONDS));
        }
        var stored = challengeParticipations.find(challenge.id(), user).orElseThrow();
        assertTrue(stored.equals(first) || stored.equals(second));
    }

    private boolean tryParticipationUpdate(ChallengeParticipation original, ChallengeParticipation revised,
                                           java.util.concurrent.CountDownLatch ready,
                                           java.util.concurrent.CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            challengeParticipations.update(original, revised);
            return true;
        } catch (ChallengeConflictException expected) {
            return false;
        }
    }

    @Test
    void serializesConcurrentParticipationCreation() throws Exception {
        UUID user = userRepository.save(User.anonymous()).id();
        UUID badge = UUID.randomUUID();
        Instant at = Instant.parse("2026-06-15T12:00:00Z");
        jdbc.update("insert into correai.badge_templates (id, name, description, icon, color, created_by, created_at) "
                        + "values (?, ?, ?, ?, ?, ?, ?)", badge, "Steps", "Walk", "FOOTPRINT", "#12AB34",
                user, java.sql.Timestamp.from(at));
        var challenge = challengeDrafts.create(user, new ChallengeDraftCommand("Walk", "Walk outside",
                ChallengeAudience.FRIENDS, null, ChallengeMetric.DISTANCE_KM, 20,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), "America/Sao_Paulo", badge));
        jdbc.update("update correai.shared_challenges set published_at = ? where id = ?",
                java.sql.Timestamp.from(at), challenge.id());
        var first = new ChallengeParticipation(challenge.id(), user, at, new ChallengeProgress(5, null), false);
        var second = new ChallengeParticipation(challenge.id(), user, at.plusSeconds(1),
                new ChallengeProgress(6, null), false);
        var ready = new java.util.concurrent.CountDownLatch(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var one = executor.submit(() -> {
                ready.countDown();
                start.await();
                return challengeParticipations.create(first);
            });
            var two = executor.submit(() -> {
                ready.countDown();
                start.await();
                return challengeParticipations.create(second);
            });
            assertTrue(ready.await(5, java.util.concurrent.TimeUnit.SECONDS));
            start.countDown();
            var result = one.get(10, java.util.concurrent.TimeUnit.SECONDS);
            assertEquals(result, two.get(10, java.util.concurrent.TimeUnit.SECONDS));
            assertTrue(result.equals(first) || result.equals(second));
        }
        assertEquals(1, challengeParticipations.findByUserId(user).size());
    }

    @Test
    void storesPrivateChallengeDraftAgainstPostgres() {
        UUID user = userRepository.save(User.anonymous()).id();
        UUID other = userRepository.save(User.anonymous()).id();
        UUID badge = UUID.randomUUID();
        jdbc.update("insert into correai.badge_templates (id, name, description, icon, color, created_by, created_at) "
                        + "values (?, ?, ?, ?, ?, ?, ?)", badge, "Steps", "Walk", "FOOTPRINT", "#12AB34",
                user, java.sql.Timestamp.from(Instant.parse("2026-06-01T12:00:00Z")));
        var command = new ChallengeDraftCommand("Walk", "Walk outside", ChallengeAudience.FRIENDS, null,
                ChallengeMetric.DISTANCE_KM, 20, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
                "America/Sao_Paulo", badge);
        var saved = challengeDrafts.create(user, command);
        assertEquals(saved, challengeDrafts.getOwn(user, saved.id()));
        assertThrows(com.correai.api.domain.model.challenge.ChallengeNotFoundException.class,
                () -> challengeDrafts.getOwn(other, saved.id()));
        assertEquals(1, challengeDrafts.listOwn(user, new PageQuery(0, 20)).totalElements());
    }

    @Test
    void storesBadgeTemplateWithFlywaySchema() {
        UUID user = userRepository.save(User.anonymous()).id();
        UUID id = UUID.randomUUID();
        jdbc.update("insert into correai.badge_templates (id, name, description, icon, color, created_by, created_at) "
                        + "values (?, ?, ?, ?, ?, ?, ?)", id, "Steps", "Walk", "FOOTPRINT", "#12AB34",
                user, java.sql.Timestamp.from(Instant.parse("2026-06-01T12:00:00Z")));
        assertEquals(BadgeIcon.FOOTPRINT, badgeTemplates.get(id).icon());
        assertEquals("#12AB34", badgeTemplates.get(id).color());
    }

    @Test
    void bootstrapIsAtomicAcrossConcurrentRequests() throws Exception {
        UUID first = userRepository.saveWithPassword(User.registered("First", UUID.randomUUID() + "@example.test"), "hash").id();
        UUID second = userRepository.saveWithPassword(User.registered("Second", UUID.randomUUID() + "@example.test"), "hash").id();
        var ready = new java.util.concurrent.CountDownLatch(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var one = executor.submit(() -> bootstrapResult(first, ready, start));
            var two = executor.submit(() -> bootstrapResult(second, ready, start));
            assertTrue(ready.await(5, java.util.concurrent.TimeUnit.SECONDS));
            start.countDown();
            assertNotEquals(one.get(10, java.util.concurrent.TimeUnit.SECONDS),
                    two.get(10, java.util.concurrent.TimeUnit.SECONDS));
        }
        assertNotEquals(admins.isAdmin(first), admins.isAdmin(second));
        assertEquals(1, jdbc.queryForObject("select count(*) from correai.admin_audit where action = 'BOOTSTRAP'",
                Integer.class));
    }

    private boolean bootstrapResult(UUID user, java.util.concurrent.CountDownLatch ready,
                                    java.util.concurrent.CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            admins.bootstrap(user);
            return true;
        } catch (AdminConflictException expected) {
            return false;
        }
    }

    @Test
    void serializesConcurrentFriendshipRequestsAndBlock() throws Exception {
        UUID first = userRepository.save(User.anonymous()).id();
        UUID second = userRepository.save(User.anonymous()).id();
        var ready = new java.util.concurrent.CountDownLatch(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var one = executor.submit(() -> {
                ready.countDown();
                start.await();
                friendships.request(first, second);
                return null;
            });
            var two = executor.submit(() -> {
                ready.countDown();
                start.await();
                friendships.request(first, second);
                return null;
            });
            assertTrue(ready.await(5, java.util.concurrent.TimeUnit.SECONDS));
            start.countDown();
            one.get(10, java.util.concurrent.TimeUnit.SECONDS);
            two.get(10, java.util.concurrent.TimeUnit.SECONDS);
        }
        assertEquals(1, friendships.list(first, new PageQuery(0, 20)).totalElements());
        friendships.accept(second, first);
        assertEquals(FriendshipState.ACCEPTED,
                friendships.list(first, new PageQuery(0, 20)).content().getFirst().state());
        friendships.block(first, second);
        assertEquals(0, friendships.list(first, new PageQuery(0, 20)).totalElements());
        assertThrows(com.correai.api.domain.model.friendship.FriendshipNotFoundException.class,
                () -> friendships.request(second, first));
    }

    @Test
    void persistsGroupMembershipAgainstPostgres() {
        UUID owner = userRepository.save(User.anonymous()).id();
        UUID participant = userRepository.save(User.anonymous()).id();
        var group = groups.create(owner, "Group " + UUID.randomUUID());
        groups.join(participant, group.id());
        groups.join(participant, group.id());
        var results = groups.search(participant, group.name(), new PageQuery(0, 20));
        assertEquals(1, results.totalElements());
        assertTrue(results.content().getFirst().member());
        groups.leave(participant, group.id());
        assertFalse(groups.search(participant, group.name(), new PageQuery(0, 20)).content().getFirst().member());
    }

    @Test
    void achievementsPersistAndAtomicUpdatesCannotRemoveOrReplaceThem() {
        UUID user = userRepository.save(User.anonymous()).id();
        UUID other = userRepository.save(User.anonymous()).id();
        Goal goal = goalRepository.save(Goal.create(user, GoalType.DISTANCE_KM, GoalPeriod.WEEKLY, 5));
        Goal awarded = goalRepository.award(goal.achieve(Instant.parse("2026-11-27T12:00:00Z"),
                LocalDate.of(2026, 11, 23), 6));
        assertTrue(awarded.hasBadge());
        assertEquals(awarded, goalRepository.findAwardedByUserId(user).getFirst());
        assertTrue(goalRepository.findAwardedByUserId(other).isEmpty());
        assertThrows(com.correai.api.domain.model.goal.GoalHasBadgeException.class,
                () -> goalRepository.deactivateUnawarded(goal.deactivate()));
        Goal repeated = goalRepository.award(goal.achieve(Instant.parse("2026-12-04T12:00:00Z"),
                LocalDate.of(2026, 11, 30), 10));
        assertEquals(awarded, repeated);
    }

    @Test
    void storesMediaAndDeletesActivityFilesWithActivity() {
        UUID userId = userRepository.save(User.anonymous()).id();
        Activity activity = activityRepository.save(Activity.createWithStartedAt(userId, ActivityType.RUN, 1, 300,
                null, null, null,
                OffsetDateTime.of(2026, 11, 27, 22, 0, 0, 0, ZoneOffset.ofHours(-3))));
        assertEquals(LocalDate.of(2026, 11, 27), activity.activityDate());
        assertEquals(LocalTime.of(22, 0), activity.startTime());
        assertEquals(Instant.parse("2026-11-28T01:00:00Z"), activity.startedAt());
        byte[] bytes = new byte[] {1, 2, 3};
        mediaRepository.save(userId, activity.id(), MediaKind.ACTIVITY_ROUTE,
                new MediaContent("application/gpx+xml", bytes));
        assertArrayEquals(bytes, mediaRepository.find(userId, activity.id(), MediaKind.ACTIVITY_ROUTE)
                .orElseThrow().bytes());
        assertEquals(java.util.List.of(activity.id()), mediaRepository.listRouteActivityIds(userId));
        assertTrue(mediaRepository.listRouteActivityIds(UUID.randomUUID()).isEmpty());
        UUID photoId = UUID.randomUUID();
        mediaRepository.save(userId, activity.id(), MediaKind.ACTIVITY_PHOTO,
                new MediaContent("image/png", bytes));
        mediaRepository.saveActivityPhoto(userId, activity.id(), photoId, new MediaContent("image/png", bytes));
        mediaRepository.saveActivityPhoto(userId, activity.id(), photoId, new MediaContent("image/png", bytes));
        assertEquals(java.util.Set.of(activity.id(), photoId),
                new java.util.HashSet<>(mediaRepository.listActivityPhotos(userId, activity.id())));
        assertEquals(2, mediaRepository.listActivityPhotos(userId, activity.id()).size());
        mediaRepository.deleteActivityPhoto(UUID.randomUUID(), activity.id(), photoId);
        assertEquals(2, mediaRepository.listActivityPhotos(userId, activity.id()).size());
        mediaRepository.deleteActivityPhoto(userId, activity.id(), activity.id());
        assertEquals(java.util.List.of(photoId), mediaRepository.listActivityPhotos(userId, activity.id()));
        mediaRepository.deleteActivityPhoto(userId, activity.id(), activity.id());
        assertArrayEquals(bytes, mediaRepository.findActivityPhoto(userId, activity.id(), photoId).orElseThrow().bytes());
        Activity edited = activityRepository.save(activity.update(ActivityType.WALK, 2, 600, null, null, "edited"));
        assertEquals(activity.id(), edited.id());
        assertEquals(activity.createdAt(), edited.createdAt());
        assertEquals(activity.startedAt(), edited.startedAt());
        assertEquals(activity.activityDate(), edited.activityDate());
        assertEquals(300, edited.avgPaceSeconds());
        assertEquals(2.0, activityRepository.findLongestDistance(userId));
        assertEquals(1, mediaRepository.listActivityPhotos(userId, activity.id()).size());
        activityRepository.delete(activity);
        assertTrue(mediaRepository.find(userId, activity.id(), MediaKind.ACTIVITY_ROUTE).isEmpty());
        assertTrue(mediaRepository.listRouteActivityIds(userId).isEmpty());
        assertTrue(mediaRepository.listActivityPhotos(userId, activity.id()).isEmpty());
        assertTrue(mediaRepository.findActivityPhoto(userId, activity.id(), photoId).isEmpty());
    }

    @Test
    void updatesAnonymousAndRegisteredProfilesWithoutOverwritingCredentials() {
        UUID userId = userRepository.save(User.anonymous()).id();
        assertEquals("Ana", userRepository.updateProfile(userId, "Ana", null, null).name());
        userRepository.registerCredentials(userId, "Ana", "profile@example.com", "hash");
        assertThrows(ConcurrentProfileUpdateException.class,
                () -> userRepository.updateProfile(userId, "Stale", null, null));
        userRepository.updateProfile(userId, "New name", "new-profile@example.com", "profile@example.com");
        userRepository.updatePasswordHash(userId, "new-hash");
        assertEquals("New name", userRepository.findById(userId).orElseThrow().name());
        assertEquals("new-hash", userRepository.findCredentialsByEmail("new-profile@example.com")
                .orElseThrow().passwordHash());
    }

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Test
    void createsTablesOnlyInDedicatedSchema() {
        Integer inPublic = jdbc.queryForObject(
                "select count(*) from information_schema.tables where table_schema = 'public'", Integer.class);
        Integer inCorreai = jdbc.queryForObject(
                "select count(*) from information_schema.tables where table_schema = 'correai' and table_name = 'users'",
                Integer.class);

        assertEquals(0, inPublic);
        assertEquals(1, inCorreai);
    }

    @Test
    void persistsAndQueriesActivitiesAgainstPostgres() {
        UUID userId = userRepository.save(User.anonymous()).id();
        LocalDate today = LocalDate.now();

        activityRepository.save(Activity.create(userId, ActivityType.RUN, 5.0, 1500,
                TrainingType.EASY, PerceivedEffort.OK, "older", today.minusDays(1)));
        activityRepository.save(Activity.create(userId, ActivityType.RUN, 12.0, 4000,
                TrainingType.LONG, PerceivedEffort.HARD, "latest", today));

        assertTrue(userRepository.existsById(userId));
        assertTrue(activityRepository.existsByUserIdAndActivityDate(userId, today));
        assertEquals(12.0, activityRepository.findLongestDistance(userId));
        assertEquals(2, activityRepository
                .findByUserIdAndActivityDateBetween(userId, today.minusDays(1), today).size());

        PageResult<Activity> page = activityRepository
                .findByUserIdOrderByActivityDateDesc(userId, new PageQuery(0, 1));
        assertEquals(2, page.totalElements());
        assertEquals(2, page.totalPages());
        assertEquals("latest", page.content().getFirst().notes());
        assertNotNull(page.content().getFirst().createdAt());
    }

    @Test
    void persistsAndQueriesGoalsAgainstPostgres() {
        UUID userId = userRepository.save(User.anonymous()).id();
        UUID otherUserId = userRepository.save(User.anonymous()).id();

        Goal saved = goalRepository.save(Goal.create(userId, GoalType.AVG_PACE, GoalPeriod.WEEKLY, 330));
        Goal inactive = goalRepository.save(Goal.create(userId, GoalType.ACTIVITIES, GoalPeriod.MONTHLY, 12).deactivate());

        assertNotNull(saved.id());
        assertNotNull(saved.createdAt());
        assertEquals(1, goalRepository.findActiveByUserId(userId).size());
        assertTrue(goalRepository.findByIdAndUserId(saved.id(), userId).isPresent());
        assertTrue(goalRepository.findByIdAndUserId(saved.id(), otherUserId).isEmpty());
        assertFalse(inactive.active());
    }
}
