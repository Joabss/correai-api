package com.correai.api.adapter.in.web.goal;

import com.correai.api.adapter.in.web.config.UserContextInterceptor;
import com.correai.api.adapter.in.web.config.WebConfig;
import com.correai.api.domain.model.goal.Goal;
import com.correai.api.domain.model.goal.GoalNotFoundException;
import com.correai.api.domain.model.goal.GoalPeriod;
import com.correai.api.domain.model.goal.GoalType;
import com.correai.api.domain.port.in.goal.CreateGoalUseCase;
import com.correai.api.domain.port.in.goal.DeactivateGoalUseCase;
import com.correai.api.domain.port.in.goal.GoalProgress;
import com.correai.api.domain.port.in.goal.ListGoalsUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = GoalController.class,
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = {UserContextInterceptor.class, WebConfig.class}))
class GoalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateGoalUseCase createGoalUseCase;

    @MockitoBean
    private ListGoalsUseCase listGoalsUseCase;

    @MockitoBean
    private DeactivateGoalUseCase deactivateGoalUseCase;

    private final UUID userId = UUID.randomUUID();

    @Test
    void create_shouldReturnCreatedGoal() throws Exception {
        Goal goal = Goal.reconstruct(UUID.randomUUID(), userId, GoalType.DISTANCE_KM, GoalPeriod.WEEKLY, 30, true, Instant.now());
        when(createGoalUseCase.create(any())).thenReturn(goal);

        mockMvc.perform(post("/goals")
                        .requestAttr("userId", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"DISTANCE_KM\",\"period\":\"WEEKLY\",\"target\":30}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(goal.id().toString()))
                .andExpect(jsonPath("$.type").value("DISTANCE_KM"))
                .andExpect(jsonPath("$.target").value(30.0));
    }

    @Test
    void create_withInvalidBody_shouldReturnBadRequestWithFieldErrors() throws Exception {
        mockMvc.perform(post("/goals")
                        .requestAttr("userId", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"period\":\"WEEKLY\",\"target\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.type").exists())
                .andExpect(jsonPath("$.errors.target").exists());
    }

    @Test
    void list_shouldReturnGoalsWithProgress() throws Exception {
        Goal km = Goal.reconstruct(UUID.randomUUID(), userId, GoalType.DISTANCE_KM, GoalPeriod.WEEKLY, 20, true, Instant.now());
        Goal pace = Goal.reconstruct(UUID.randomUUID(), userId, GoalType.AVG_PACE, GoalPeriod.MONTHLY, 330, true, Instant.now());
        LocalDate start = LocalDate.of(2026, 9, 28);
        when(listGoalsUseCase.listActive(userId)).thenReturn(List.of(
                new GoalProgress(km, start, start.plusDays(6), 10.0, 50.0, false),
                new GoalProgress(pace, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), 300.0, null, true)));

        mockMvc.perform(get("/goals").requestAttr("userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].current").value(10.0))
                .andExpect(jsonPath("$[0].percentage").value(50.0))
                .andExpect(jsonPath("$[0].achieved").value(false))
                .andExpect(jsonPath("$[0].targetPace").doesNotExist())
                .andExpect(jsonPath("$[1].targetPace").value("05:30"))
                .andExpect(jsonPath("$[1].currentPace").value("05:00"))
                .andExpect(jsonPath("$[1].achieved").value(true));
    }

    @Test
    void deactivate_shouldReturnNoContent() throws Exception {
        UUID goalId = UUID.randomUUID();

        mockMvc.perform(delete("/goals/{id}", goalId).requestAttr("userId", userId))
                .andExpect(status().isNoContent());

        verify(deactivateGoalUseCase).deactivate(userId, goalId);
    }

    @Test
    void deactivate_withUnknownGoal_shouldReturnNotFound() throws Exception {
        UUID goalId = UUID.randomUUID();
        doThrow(new GoalNotFoundException(goalId)).when(deactivateGoalUseCase).deactivate(userId, goalId);

        mockMvc.perform(delete("/goals/{id}", goalId).requestAttr("userId", userId))
                .andExpect(status().isNotFound());
    }
}
