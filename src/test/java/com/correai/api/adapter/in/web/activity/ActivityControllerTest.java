package com.correai.api.adapter.in.web.activity;

import com.correai.api.domain.model.activity.Activity;
import com.correai.api.domain.model.activity.ActivityType;
import com.correai.api.domain.model.activity.PerceivedEffort;
import com.correai.api.domain.model.activity.TrainingType;
import com.correai.api.domain.model.pagination.PageQuery;
import com.correai.api.domain.model.pagination.PageResult;
import com.correai.api.domain.port.in.activity.ActivityCreationResult;
import com.correai.api.domain.port.in.activity.CreateActivityUseCase;
import com.correai.api.domain.port.in.activity.DeleteActivityUseCase;
import com.correai.api.domain.port.in.activity.UpdateActivityUseCase;
import com.correai.api.domain.port.in.activity.GetActivityUseCase;
import com.correai.api.domain.model.activity.ActivityNotFoundException;
import com.correai.api.domain.port.in.activity.ListActivitiesUseCase;
import com.correai.api.adapter.in.web.activity.dto.ActivityRequest;
import com.correai.api.adapter.in.web.config.UserContextInterceptor;
import com.correai.api.adapter.in.web.config.WebConfig;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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

    @MockitoBean
    private GetActivityUseCase getActivityUseCase;

    @MockitoBean
    private DeleteActivityUseCase deleteActivityUseCase;

    @MockitoBean
    private UpdateActivityUseCase updateActivityUseCase;

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

        activity = Activity.reconstructWithStartedAt(UUID.randomUUID(), userId, ActivityType.RUN, LocalDate.now(),
                LocalTime.of(22, 0), Instant.parse("2026-11-28T01:00:00Z"),
                10.0, 3600, 360, TrainingType.EASY, PerceivedEffort.OK, "Test run", Instant.now());
    }

    @Test
    void updateReturnsActivityAndPassesAuthenticatedOwner() throws Exception {
        when(updateActivityUseCase.update(any())).thenReturn(activity);
        mockMvc.perform(put("/activities/{id}", activity.id()).requestAttr("userId", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"WALK\",\"distanceKm\":5,\"durationSeconds\":2400,\"notes\":\"updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(activity.id().toString()))
                .andExpect(jsonPath("$.startedAt").value("2026-11-28T01:00:00Z"));
        verify(updateActivityUseCase).update(argThat(command -> command.userId().equals(userId)
                && command.activityId().equals(activity.id()) && command.type() == ActivityType.WALK
                && command.distanceKm() == 5 && command.durationSeconds() == 2400
                && command.trainingType() == null && command.notes().equals("updated")));
    }

    @Test
    void updateRejectsInvalidPayloads() throws Exception {
        for (String payload : List.of("{}", "{\"type\":\"RUN\",\"distanceKm\":0,\"durationSeconds\":300}",
                "{\"type\":\"RUN\",\"distanceKm\":1,\"durationSeconds\":0}",
                "{\"type\":\"INVALID\",\"distanceKm\":1,\"durationSeconds\":300}")) {
            mockMvc.perform(put("/activities/{id}", activity.id()).requestAttr("userId", userId)
                    .contentType(MediaType.APPLICATION_JSON).content(payload))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(updateActivityUseCase);
    }

    @Test
    void updateRejectsNotesBeyondColumnLimit() throws Exception {
        mockMvc.perform(put("/activities/{id}", activity.id()).requestAttr("userId", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"RUN\",\"distanceKm\":1,\"durationSeconds\":300,\"notes\":\""
                        + "a".repeat(256) + "\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(updateActivityUseCase);
    }

    @Test
    void updateReturnsNotFoundForMissingActivity() throws Exception {
        when(updateActivityUseCase.update(any())).thenThrow(new ActivityNotFoundException());
        mockMvc.perform(put("/activities/{id}", activity.id()).requestAttr("userId", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"RUN\",\"distanceKm\":1,\"durationSeconds\":300}"))
                .andExpect(status().isNotFound());
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
                .andExpect(jsonPath("$.content[0].date").exists())
                .andExpect(jsonPath("$.content[0].startTime").value("22:00:00"))
                .andExpect(jsonPath("$.content[0].startedAt").value("2026-11-28T01:00:00Z"))
                .andExpect(jsonPath("$.content[0].distanceKm").value(10.0))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(listActivitiesUseCase).list(userId, new PageQuery(0, 20));
    }

    @Test
    void get_shouldReturnActivityDetail() throws Exception {
        when(getActivityUseCase.get(userId, activity.id())).thenReturn(activity);

        mockMvc.perform(get("/activities/{id}", activity.id()).requestAttr("userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(activity.id().toString()))
                .andExpect(jsonPath("$.notes").value("Test run"))
                .andExpect(jsonPath("$.startTime").value("22:00:00"))
                .andExpect(jsonPath("$.trainingType").value("EASY"));
    }

    @Test
    void get_unknownActivity_shouldReturnNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(getActivityUseCase.get(userId, id)).thenThrow(new ActivityNotFoundException());

        mockMvc.perform(get("/activities/{id}", id).requestAttr("userId", userId))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_shouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/activities/{id}", activity.id()).requestAttr("userId", userId))
                .andExpect(status().isNoContent());

        verify(deleteActivityUseCase).delete(userId, activity.id());
    }

    @Test
    void list_withInvalidSize_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/activities")
                        .param("size", "0")
                        .requestAttr("userId", userId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void create_withInvalidBody_shouldReturnProblemDetailWithFieldErrors() throws Exception {
        request.setDistanceKm(-1.0);

        mockMvc.perform(post("/activities")
                        .requestAttr("userId", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.distanceKm").exists());
    }

    @Test
    void create_shouldReturnCreatedActivity() throws Exception {
        ActivityCreationResult result = new ActivityCreationResult(activity, 10.0, 1, List.of(), List.of());
        when(createActivityUseCase.create(any())).thenReturn(result);
        OffsetDateTime startedAt = OffsetDateTime.of(2026, 11, 27, 22, 0, 0, 0, ZoneOffset.ofHours(-3));
        request.setStartedAt(startedAt.toString());

        mockMvc.perform(post("/activities")
                        .requestAttr("userId", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(activity.id().toString()))
                .andExpect(jsonPath("$.avgPace").value("06:00"))
                .andExpect(jsonPath("$.totalKmMonth").value(10.0))
                .andExpect(jsonPath("$.streak").value(1));

        var command = org.mockito.ArgumentCaptor.forClass(
                com.correai.api.domain.port.in.activity.CreateActivityCommand.class);
        verify(createActivityUseCase).create(command.capture());
        org.junit.jupiter.api.Assertions.assertEquals(startedAt, command.getValue().startedAt());
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

    @Test
    void create_withLegacyStartedAtWithoutOffset_shouldAssumeSaoPaulo() throws Exception {
        when(createActivityUseCase.create(any()))
                .thenReturn(new ActivityCreationResult(activity, 10.0, 1, List.of(), List.of()));
        request.setStartedAt("2026-11-27T22:00:00");

        mockMvc.perform(post("/activities")
                        .requestAttr("userId", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        var command = org.mockito.ArgumentCaptor.forClass(
                com.correai.api.domain.port.in.activity.CreateActivityCommand.class);
        verify(createActivityUseCase).create(command.capture());
        org.junit.jupiter.api.Assertions.assertEquals(
                OffsetDateTime.of(2026, 11, 27, 22, 0, 0, 0, ZoneOffset.ofHours(-3)),
                command.getValue().startedAt());
    }

    @Test
    void create_withMalformedStartedAt_shouldReturnBadRequest() throws Exception {
        request.setStartedAt("not-a-date");

        mockMvc.perform(post("/activities")
                        .requestAttr("userId", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").exists());

        verify(createActivityUseCase, never()).create(any());
    }
}
