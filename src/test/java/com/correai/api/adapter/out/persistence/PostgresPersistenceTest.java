package com.correai.api.adapter.out.persistence;

import com.correai.api.domain.model.activity.Activity;
import com.correai.api.domain.model.activity.ActivityType;
import com.correai.api.domain.model.activity.PerceivedEffort;
import com.correai.api.domain.model.activity.TrainingType;
import com.correai.api.domain.model.pagination.PageQuery;
import com.correai.api.domain.model.pagination.PageResult;
import com.correai.api.domain.model.user.User;
import com.correai.api.domain.port.out.activity.ActivityRepositoryPort;
import com.correai.api.domain.port.out.user.UserRepositoryPort;
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
}
