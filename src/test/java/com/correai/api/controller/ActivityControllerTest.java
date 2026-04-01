package com.correai.api.controller;

import com.correai.api.domain.activity.ActivityType;
import com.correai.api.domain.activity.PerceivedEffort;
import com.correai.api.domain.activity.TrainingType;
import com.correai.api.dto.ActivityItemResponse;
import com.correai.api.dto.ActivityRequest;
import com.correai.api.dto.ActivityResponse;
import com.correai.api.repository.UserRepository;
import com.correai.api.service.ActivityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ActivityController.class)
class ActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActivityService service;

    @MockitoBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID userId;
    private ActivityRequest request;
    private ActivityResponse response;
    private ActivityItemResponse itemResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        request = new ActivityRequest();
        request.setType(ActivityType.RUN);
        request.setDistanceKm(10.0);
        request.setDurationSeconds(3600);
        request.setTrainingType(TrainingType.EASY);
        request.setPerceivedEffort(PerceivedEffort.OK);
        request.setNotes("Test run");

        response = new ActivityResponse();
        response.setId(UUID.randomUUID());
        response.setAvgPace("06:00");
        response.setTotalKmMonth(10.0);
        response.setStreak(1);
        response.setNewBadges(List.of());

        itemResponse = new ActivityItemResponse();
        itemResponse.setId(UUID.randomUUID());
        itemResponse.setType(ActivityType.RUN);
        itemResponse.setDate(LocalDate.now());
        itemResponse.setDistanceKm(10.0);
        itemResponse.setDurationSeconds(3600);
        itemResponse.setAvgPace("06:00");
    }

    @Test
    void list_shouldReturnListOfActivities() throws Exception {
        when(service.list(userId)).thenReturn(List.of(itemResponse));

        mockMvc.perform(get("/activities")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(itemResponse.getId().toString()))
                .andExpect(jsonPath("$[0].type").value("RUN"))
                .andExpect(jsonPath("$[0].distanceKm").value(10.0));

        verify(service).list(userId);
    }

    @Test
    void create_shouldReturnCreatedActivity() throws Exception {
        when(service.create(eq(userId), any(ActivityRequest.class))).thenReturn(response);

        mockMvc.perform(post("/activities")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(response.getId().toString()))
                .andExpect(jsonPath("$.avgPace").value("06:00"))
                .andExpect(jsonPath("$.totalKmMonth").value(10.0))
                .andExpect(jsonPath("$.streak").value(1));

        verify(service).create(eq(userId), any(ActivityRequest.class));
    }

    @Test
    void create_withInvalidRequest_shouldReturnBadRequest() throws Exception {
        request.setDistanceKm(0.0);

        mockMvc.perform(post("/activities")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(service, never()).create(any(UUID.class), any(ActivityRequest.class));
    }
}
