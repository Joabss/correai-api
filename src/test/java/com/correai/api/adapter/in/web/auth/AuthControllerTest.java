package com.correai.api.adapter.in.web.auth;

import com.correai.api.adapter.in.web.config.UserContextInterceptor;
import com.correai.api.adapter.in.web.config.WebConfig;
import com.correai.api.domain.model.auth.AuthToken;
import com.correai.api.domain.port.in.auth.IssueAnonymousTokenUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class,
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = {UserContextInterceptor.class, WebConfig.class}))
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IssueAnonymousTokenUseCase issueAnonymousTokenUseCase;

    @Test
    void anonymous_shouldReturnCreatedWithBearerToken() throws Exception {
        UUID userId = UUID.randomUUID();
        when(issueAnonymousTokenUseCase.issue())
                .thenReturn(new AuthToken("jwt-value", userId, Instant.parse("2030-01-01T00:00:00Z")));

        mockMvc.perform(post("/auth/anonymous"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("jwt-value"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.userId").value(userId.toString()));
    }
}
