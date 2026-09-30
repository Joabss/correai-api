package com.correai.api.config;

import com.correai.api.application.activity.ActivityApplicationService;
import com.correai.api.application.stats.StatsApplicationService;
import com.correai.api.application.user.UserContextApplicationService;
import com.correai.api.domain.port.out.activity.ActivityRepositoryPort;
import com.correai.api.domain.port.out.user.UserRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfig {

    @Bean
    public ActivityApplicationService activityApplicationService(ActivityRepositoryPort repository) {
        return new ActivityApplicationService(repository);
    }

    @Bean
    public StatsApplicationService statsApplicationService(ActivityRepositoryPort repository) {
        return new StatsApplicationService(repository);
    }

    @Bean
    public UserContextApplicationService userContextApplicationService(UserRepositoryPort repository) {
        return new UserContextApplicationService(repository);
    }
}
