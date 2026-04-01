package com.correai.api.controller;

import com.correai.api.dto.StatsSummaryResponse;
import com.correai.api.repository.UserRepository;
import com.correai.api.service.StatsService;
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

@WebMvcTest(StatsController.class)
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatsService service;

    @MockitoBean
    private UserRepository userRepository;

    private UUID userId;
    private StatsSummaryResponse response;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        response = new StatsSummaryResponse();
        response.setKmWeek(15.0);
        response.setKmMonth(50.0);
        response.setActivitiesWeek(3);
        response.setStreak(5);
        response.setLongestDistance(10.0);
    }

    @Test
    void getSummary_shouldReturnStatsSummary() throws Exception {
        when(service.getSummary(userId)).thenReturn(response);

        mockMvc.perform(get("/stats/summary")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kmWeek").value(15.0))
                .andExpect(jsonPath("$.kmMonth").value(50.0))
                .andExpect(jsonPath("$.activitiesWeek").value(3))
                .andExpect(jsonPath("$.streak").value(5))
                .andExpect(jsonPath("$.longestDistance").value(10.0));

        verify(service).getSummary(userId);
    }
}
