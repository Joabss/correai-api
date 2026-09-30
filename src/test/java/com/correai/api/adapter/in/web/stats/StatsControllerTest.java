package com.correai.api.adapter.in.web.stats;

import com.correai.api.domain.port.in.stats.GetStatsSummaryUseCase;
import com.correai.api.domain.port.in.stats.StatsSummary;
import com.correai.api.adapter.in.web.config.UserContextInterceptor;
import com.correai.api.adapter.in.web.config.WebConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = StatsController.class,
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = {UserContextInterceptor.class, WebConfig.class}))
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetStatsSummaryUseCase getStatsSummaryUseCase;

    private UUID userId;
    private StatsSummary summary;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        summary = new StatsSummary(15.0, 50.0, 3, 5, 10.0);
    }

    @Test
    void getSummary_shouldReturnStatsSummary() throws Exception {
        when(getStatsSummaryUseCase.getSummary(userId)).thenReturn(summary);

        mockMvc.perform(get("/stats/summary")
                        .requestAttr("userId", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kmWeek").value(15.0))
                .andExpect(jsonPath("$.kmMonth").value(50.0))
                .andExpect(jsonPath("$.activitiesWeek").value(3))
                .andExpect(jsonPath("$.streak").value(5))
                .andExpect(jsonPath("$.longestDistance").value(10.0));

        verify(getStatsSummaryUseCase).getSummary(userId);
    }
}

