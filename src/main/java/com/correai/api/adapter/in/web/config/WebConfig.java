package com.correai.api.adapter.in.web.config;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final UserContextInterceptor interceptor;
    private final String[] allowedOrigins;

    public WebConfig(
            UserContextInterceptor interceptor,
            @Value("${app.cors.allowed-origins:}") String[] allowedOrigins
    ) {
        this.interceptor = interceptor;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor)
                .addPathPatterns("/activities/**", "/stats/**")
                .excludePathPatterns(
                        "/actuator/**",
                        "/error"
                );
    }

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        if (allowedOrigins.length == 0) {
            return;
        }
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type")
                .maxAge(3600);
    }
}
