package com.correai.api.adapter.in.web.auth;

import com.correai.api.adapter.in.web.config.UserContextInterceptor;
import com.correai.api.adapter.in.web.config.WebConfig;
import com.correai.api.domain.model.auth.AuthToken;
import com.correai.api.domain.model.auth.InvalidCredentialsException;
import com.correai.api.domain.model.auth.InvalidTokenException;
import com.correai.api.domain.port.in.auth.IssueAnonymousTokenUseCase;
import com.correai.api.domain.port.in.auth.LoginUseCase;
import com.correai.api.domain.port.in.auth.PasswordResetUseCase;
import com.correai.api.domain.port.in.auth.RefreshTokenUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;
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

    @MockitoBean
    private LoginUseCase loginUseCase;

    @MockitoBean
    private RefreshTokenUseCase refreshTokenUseCase;

    @MockitoBean
    private PasswordResetUseCase passwordResetUseCase;

    @Test
    void refresh_shouldReturnRotatedTokens() throws Exception {
        UUID userId = UUID.randomUUID();
        when(refreshTokenUseCase.refresh("old"))
                .thenReturn(new AuthToken("jwt-2", userId, Instant.parse("2030-01-01T00:00:00Z"), "new"));

        mockMvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"old\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-2"))
                .andExpect(jsonPath("$.refreshToken").value("new"));
    }

    @Test
    void refresh_withInvalidToken_shouldReturnUnauthorized() throws Exception {
        when(refreshTokenUseCase.refresh("bad")).thenThrow(new InvalidTokenException("Invalid or expired refresh token"));

        mockMvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"bad\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void passwordResetRequest_shouldReturnAccepted() throws Exception {
        mockMvc.perform(post("/auth/password-reset/request").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@example.com\"}"))
                .andExpect(status().isAccepted());

        verify(passwordResetUseCase).request("ana@example.com");
    }

    @Test
    void passwordResetConfirm_shouldReturnNoContent() throws Exception {
        mockMvc.perform(post("/auth/password-reset/confirm").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"t\",\"newPassword\":\"new-secret-1\"}"))
                .andExpect(status().isNoContent());

        verify(passwordResetUseCase).reset("t", "new-secret-1");
    }

    @Test
    void passwordResetConfirm_withShortPassword_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/auth/password-reset/confirm").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"t\",\"newPassword\":\"short\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_shouldReturnToken() throws Exception {
        UUID userId = UUID.randomUUID();
        when(loginUseCase.login("ana@example.com", "secret-123"))
                .thenReturn(new AuthToken("jwt-value", userId, Instant.parse("2030-01-01T00:00:00Z")));

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@example.com\",\"password\":\"secret-123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-value"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    void login_withWrongCredentials_shouldReturnUnauthorized() throws Exception {
        when(loginUseCase.login("ana@example.com", "wrong"))
                .thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_withMissingFields_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

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
