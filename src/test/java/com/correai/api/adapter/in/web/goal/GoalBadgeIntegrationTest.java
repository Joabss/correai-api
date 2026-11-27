package com.correai.api.adapter.in.web.goal;

import com.correai.api.domain.model.activity.ActivityType;
import com.correai.api.domain.model.auth.AuthToken;
import com.correai.api.domain.model.goal.GoalPeriod;
import com.correai.api.domain.model.goal.GoalType;
import com.correai.api.domain.port.in.activity.CreateActivityCommand;
import com.correai.api.domain.port.in.activity.CreateActivityUseCase;
import com.correai.api.domain.port.in.auth.IssueAnonymousTokenUseCase;
import com.correai.api.domain.port.in.goal.CreateGoalCommand;
import com.correai.api.domain.port.in.goal.CreateGoalUseCase;
import com.correai.api.domain.port.out.goal.GoalRepositoryPort;
import com.correai.api.domain.port.out.activity.ActivityRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class GoalBadgeIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired IssueAnonymousTokenUseCase tokens;
    @Autowired CreateGoalUseCase challenges;
    @Autowired CreateActivityUseCase activities;
    @MockitoSpyBean GoalRepositoryPort goals;
    @Autowired ActivityRepositoryPort activityRepository;

    @Test
    void refusingDeletionCommitsPreviouslyUnobservedAchievement() throws Exception {
        AuthToken owner = tokens.issue();
        var goal = goals.save(com.correai.api.domain.model.goal.Goal.create(owner.userId(),
                GoalType.ACTIVITIES, GoalPeriod.WEEKLY, 1));
        activityRepository.save(com.correai.api.domain.model.activity.Activity.create(owner.userId(),
                ActivityType.WALK, 5, 1800, null, null, null, java.time.LocalDate.now()));
        mvc.perform(delete("/goals/" + goal.id()).header("Authorization", "Bearer " + owner.token()))
                .andExpect(status().isConflict());
        assertTrue(goals.findByIdAndUserId(goal.id(), owner.userId()).orElseThrow().hasBadge());
    }

    @Test
    void badgePersistenceFailureRollsBackActivityCreation() throws Exception {
        AuthToken owner = tokens.issue();
        challenges.create(new CreateGoalCommand(owner.userId(), GoalType.ACTIVITIES, GoalPeriod.WEEKLY, 1));
        doThrow(new IllegalStateException("Simulated badge persistence failure")).when(goals).award(any());
        mvc.perform(post("/activities").header("Authorization", "Bearer " + owner.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"WALK\",\"distanceKm\":5,\"durationSeconds\":1800}"))
                .andExpect(status().isInternalServerError());
        assertTrue(activityRepository.findByUserIdAndActivityDateBetween(owner.userId(),
                java.time.LocalDate.now().minusDays(1), java.time.LocalDate.now().plusDays(1)).isEmpty());
        assertTrue(goals.findAwardedByUserId(owner.userId()).isEmpty());
    }

    @Test
    void goalCompletionSurvivesEditDeletionAndReplacementAndIsPrivate() throws Exception {
        AuthToken owner = tokens.issue();
        AuthToken other = tokens.issue();
        var goal = challenges.create(new CreateGoalCommand(owner.userId(), GoalType.ACTIVITIES, GoalPeriod.WEEKLY, 1));
        var result = activities.create(new CreateActivityCommand(owner.userId(), ActivityType.WALK,
                5, 1800, null, null, null, null));
        var activity = result.activity();
        assertEquals(java.util.List.of(goal.id().toString()), result.newBadges());
        var awarded = goals.findByIdAndUserId(goal.id(), owner.userId()).orElseThrow();
        assertTrue(awarded.hasBadge());
        String auth = "Bearer " + owner.token();
        String badge = "/goals/" + goal.id() + "/badge";
        mvc.perform(get(badge)).andExpect(status().isUnauthorized());
        mvc.perform(get(badge).header("Authorization", "Bearer " + other.token())).andExpect(status().isNotFound());
        mvc.perform(get("/goals/badges").header("Authorization", "Bearer " + other.token()))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(put("/activities/" + activity.id()).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"RUN\",\"distanceKm\":1,\"durationSeconds\":400}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/activities/" + activity.id()).header("Authorization", auth))
                .andExpect(status().isNoContent());
        challenges.create(new CreateGoalCommand(owner.userId(), GoalType.ACTIVITIES, GoalPeriod.WEEKLY, 3));
        assertEquals(awarded, goals.findByIdAndUserId(goal.id(), owner.userId()).orElseThrow());
        mvc.perform(delete("/goals/" + goal.id()).header("Authorization", auth))
                .andExpect(status().isConflict());
        mvc.perform(get("/goals/badges").header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(goal.id().toString()))
                .andExpect(jsonPath("$[0].current").value(1));
        mvc.perform(get(badge).header("Authorization", auth)).andExpect(status().isNotFound());
    }
}
