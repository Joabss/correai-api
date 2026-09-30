package com.correai.api.config;

import com.correai.api.application.activity.ActivityApplicationService;
import com.correai.api.application.common.StreakCalculator;
import com.correai.api.application.goal.GoalApplicationService;
import com.correai.api.application.stats.StatsApplicationService;
import com.correai.api.application.auth.AuthApplicationService;
import com.correai.api.domain.port.out.activity.ActivityRepositoryPort;
import com.correai.api.domain.port.out.auth.TokenPort;
import com.correai.api.domain.port.out.goal.GoalRepositoryPort;
import com.correai.api.domain.port.out.user.UserRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class BeanConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public StreakCalculator streakCalculator(ActivityRepositoryPort repository, Clock clock) {
        return new StreakCalculator(repository, clock);
    }

    @Bean
    public ActivityApplicationService activityApplicationService(
            ActivityRepositoryPort repository, StreakCalculator streakCalculator, Clock clock) {
        return new ActivityApplicationService(repository, streakCalculator, clock);
    }

    @Bean
    public StatsApplicationService statsApplicationService(
            ActivityRepositoryPort repository, StreakCalculator streakCalculator, Clock clock) {
        return new StatsApplicationService(repository, streakCalculator, clock);
    }

    @Bean
    public GoalApplicationService goalApplicationService(
            GoalRepositoryPort goalRepository, ActivityRepositoryPort activityRepository, Clock clock) {
        return new GoalApplicationService(goalRepository, activityRepository, clock);
    }

    @Bean
    public AuthApplicationService authApplicationService(UserRepositoryPort repository, TokenPort tokenPort) {
        return new AuthApplicationService(repository, tokenPort);
    }
}
