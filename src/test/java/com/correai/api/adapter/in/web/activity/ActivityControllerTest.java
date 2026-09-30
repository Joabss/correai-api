package com.correai.api.adapter.in.web.activity;

import com.correai.api.domain.model.activity.Activity;
import com.correai.api.domain.model.activity.ActivityType;
import com.correai.api.domain.model.activity.PerceivedEffort;
import com.correai.api.domain.model.activity.TrainingType;
import com.correai.api.domain.model.pagination.PageQuery;
import com.correai.api.domain.model.pagination.PageResult;
import com.correai.api.domain.port.in.activity.ActivityCreationResult;
import com.correai.api.domain.port.in.activity.CreateActivityUseCase;
import com.correai.api.domain.port.in.activity.ListActivitiesUseCase;
import com.correai.api.adapter.in.web.activity.dto.ActivityRequest;
import com.correai.api.adapter.in.web.config.UserContextInterceptor;
import com.correai.api.adapter.in.web.config.WebConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ActivityController.class,
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = {UserContextInterceptor.class, WebConfig.class}))
class ActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateActivityUseCase createActivityUseCase;

    @MockitoBean
    private ListActivitiesUseCase listActivitiesUseCase;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID userId;
    private ActivityRequest request;
    private Activity activity;

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

        activity = Activity.reconstruct(UUID.randomUUID(), userId, ActivityType.RUN, LocalDate.now(),
                10.0, 3600, 360, TrainingType.EASY, PerceivedEffort.OK, "Test run", Instant.now());
    }

    @Test
    void list_shouldReturnListOfActivities() throws Exception {
        when(listActivitiesUseCase.list(userId, new PageQuery(0, 20)))
                .thenReturn(new PageResult<>(List.of(activity), 0, 20, 1));

        mockMvc.perform(get("/activities")
                        .requestAttr("userId", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(activity.id().toString()))
                .andExpect(jsonPath("$.content[0].type").value("RUN"))
                .andExpect(jsonPath("$.content[0].distanceKm").value(10.0))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(listActivitiesUseCase).list(userId, new PageQuery(0, 20));
    }

    @Test
    void list_withInvalidSize_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/activities")
                        .param("size", "0")
                        .requestAttr("userId", userId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturnCreatedActivity() throws Exception {
        ActivityCreationResult result = new ActivityCreationResult(activity, 10.0, 1, List.of());
        when(createActivityUseCase.create(any())).thenReturn(result);

        mockMvc.perform(post("/activities")
                        .requestAttr("userId", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(activity.id().toString()))
                .andExpect(jsonPath("$.avgPace").value("06:00"))
                .andExpect(jsonPath("$.totalKmMonth").value(10.0))
                .andExpect(jsonPath("$.streak").value(1));

        verify(createActivityUseCase).create(any());
    }

    @Test
    void create_withInvalidRequest_shouldReturnBadRequest() throws Exception {
        request.setDistanceKm(0.0);

        mockMvc.perform(post("/activities")
                        .requestAttr("userId", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(createActivityUseCase, never()).create(any());
    }
}

