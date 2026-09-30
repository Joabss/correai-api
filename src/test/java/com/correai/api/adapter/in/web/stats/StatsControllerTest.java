package com.correai.api.adapter.in.web.stats;

import com.correai.api.domain.port.in.stats.GetStatsEvolutionUseCase;
import com.correai.api.domain.port.in.stats.GetStatsSummaryUseCase;
import com.correai.api.domain.port.in.stats.WeeklyEvolution;
import com.correai.api.domain.port.in.stats.StatsSummary;
import com.correai.api.adapter.in.web.config.UserContextInterceptor;
import com.correai.api.adapter.in.web.config.WebConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
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

    @MockitoBean
    private GetStatsEvolutionUseCase getStatsEvolutionUseCase;

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

    @Test
    void getEvolution_shouldReturnWeeklyItems() throws Exception {
        when(getStatsEvolutionUseCase.getEvolution(userId, 2)).thenReturn(List.of(
                new WeeklyEvolution(LocalDate.of(2026, 9, 21), 0.0, 0, null),
                new WeeklyEvolution(LocalDate.of(2026, 9, 28), 10.0, 2, 360)));

        mockMvc.perform(get("/stats/evolution")
                        .param("weeks", "2")
                        .requestAttr("userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeks").value(2))
                .andExpect(jsonPath("$.items[0].weekStart").value("2026-09-21"))
                .andExpect(jsonPath("$.items[0].avgPace").doesNotExist())
                .andExpect(jsonPath("$.items[1].km").value(10.0))
                .andExpect(jsonPath("$.items[1].avgPace").value("06:00"));
    }

    @Test
    void getEvolution_withInvalidWeeks_shouldReturnBadRequest() throws Exception {
        when(getStatsEvolutionUseCase.getEvolution(userId, 0))
                .thenThrow(new IllegalArgumentException("Weeks must be between 1 and 52"));

        mockMvc.perform(get("/stats/evolution")
                        .param("weeks", "0")
                        .requestAttr("userId", userId))
                .andExpect(status().isBadRequest());
    }
}
