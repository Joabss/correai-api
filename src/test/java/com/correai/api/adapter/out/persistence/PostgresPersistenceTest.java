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

import java.time.LocalDate;
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

    @Test
    void storesMediaAndDeletesActivityFilesWithActivity() {
        UUID userId = userRepository.save(User.anonymous()).id();
        Activity activity = activityRepository.save(Activity.create(userId, ActivityType.RUN, 1, 300,
                null, null, null, LocalDate.now()));
        byte[] bytes = new byte[] {1, 2, 3};
        mediaRepository.save(userId, activity.id(), MediaKind.ACTIVITY_ROUTE,
                new MediaContent("application/gpx+xml", bytes));
        assertArrayEquals(bytes, mediaRepository.find(userId, activity.id(), MediaKind.ACTIVITY_ROUTE)
                .orElseThrow().bytes());
        activityRepository.delete(activity);
        assertTrue(mediaRepository.find(userId, activity.id(), MediaKind.ACTIVITY_ROUTE).isEmpty());
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
