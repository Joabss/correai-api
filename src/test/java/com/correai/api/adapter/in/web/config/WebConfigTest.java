package com.correai.api.adapter.in.web.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebConfigTest {

    @Mock
    private UserContextInterceptor interceptor;

    @Mock
    private InterceptorRegistry registry;

    @Mock
    private InterceptorRegistration registration;

    @Test
    void addInterceptors_shouldAddInterceptorWithPaths() {
        when(registry.addInterceptor(interceptor)).thenReturn(registration);
        when(registration.addPathPatterns(any(String[].class))).thenReturn(registration);

        WebConfig config = new WebConfig(interceptor, new String[0]);

        config.addInterceptors(registry);

        verify(registry).addInterceptor(interceptor);
        verify(registration).addPathPatterns("/activities/**", "/stats/**", "/goals/**");
        verify(registration).excludePathPatterns("/actuator/**", "/error");
    }

    @Test
    void addCorsMappings_withoutOrigins_shouldRegisterNothing() {
        CorsRegistry corsRegistry = mock(CorsRegistry.class);

        new WebConfig(interceptor, new String[0]).addCorsMappings(corsRegistry);

        verifyNoInteractions(corsRegistry);
    }

    @Test
    void addCorsMappings_withOrigins_shouldRegisterMapping() {
        class ExposedCorsRegistry extends CorsRegistry {
            Map<String, CorsConfiguration> configurations() {
                return getCorsConfigurations();
            }
        }
        ExposedCorsRegistry corsRegistry = new ExposedCorsRegistry();

        new WebConfig(interceptor, new String[]{"http://localhost:3000"}).addCorsMappings(corsRegistry);

        assertEquals(1, corsRegistry.configurations().size());
        assertEquals(List.of("http://localhost:3000"),
                corsRegistry.configurations().get("/**").getAllowedOrigins());
    }
}

