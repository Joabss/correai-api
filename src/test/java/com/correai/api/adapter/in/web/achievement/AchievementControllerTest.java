package com.correai.api.adapter.in.web.achievement;

import com.correai.api.adapter.in.web.config.UserContextInterceptor;
import com.correai.api.adapter.in.web.config.WebConfig;
import com.correai.api.domain.model.achievement.Achievement;
import com.correai.api.domain.model.achievement.BadgeCode;
import com.correai.api.domain.port.in.achievement.AchievementsUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AchievementController.class,
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = {UserContextInterceptor.class, WebConfig.class}))
class AchievementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AchievementsUseCase achievementsUseCase;

    private final UUID userId = UUID.randomUUID();

    @Test
    void list_shouldReturnCatalogWithUnlockedState() throws Exception {
        Instant unlockedAt = Instant.parse("2026-01-01T00:00:00Z");
        when(achievementsUseCase.list(userId)).thenReturn(List.of(
                Achievement.unlocked(BadgeCode.PRIMEIRO_PASSO, unlockedAt),
                Achievement.locked(BadgeCode.MARATONA)));

        mockMvc.perform(get("/achievements/me").requestAttr("userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("primeiro_passo"))
                .andExpect(jsonPath("$[0].name").value("Primeiro Passo"))
                .andExpect(jsonPath("$[0].unlocked").value(true))
                .andExpect(jsonPath("$[0].unlockedAt").exists())
                .andExpect(jsonPath("$[1].code").value("maratona"))
                .andExpect(jsonPath("$[1].unlocked").value(false))
                .andExpect(jsonPath("$[1].unlockedAt").doesNotExist());
    }
}
