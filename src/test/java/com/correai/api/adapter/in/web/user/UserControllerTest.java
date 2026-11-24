package com.correai.api.adapter.in.web.user;

import com.correai.api.adapter.in.web.config.UserContextInterceptor;
import com.correai.api.adapter.in.web.config.WebConfig;
import com.correai.api.domain.model.auth.AuthToken;
import com.correai.api.domain.model.user.AccountAlreadyRegisteredException;
import com.correai.api.domain.model.user.EmailAlreadyRegisteredException;
import com.correai.api.domain.model.user.User;
import com.correai.api.domain.port.in.user.ConvertToAccountUseCase;
import com.correai.api.domain.port.in.user.CreateUserUseCase;
import com.correai.api.domain.port.in.user.GetUserUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class,
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = {UserContextInterceptor.class, WebConfig.class}))
class UserControllerTest {

    private static final String VALID_BODY =
            "{\"name\":\"Ana\",\"email\":\"ana@example.com\",\"password\":\"secret-123\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateUserUseCase createUserUseCase;

    @MockitoBean
    private GetUserUseCase getUserUseCase;

    @MockitoBean
    private ConvertToAccountUseCase convertToAccountUseCase;

    @Test
    void convertToAccount_shouldReturnUser() throws Exception {
        UUID userId = UUID.randomUUID();
        when(convertToAccountUseCase.convertToAccount(userId, "Ana", "ana@example.com", "secret-123"))
                .thenReturn(User.reconstruct(userId, "Ana", "ana@example.com", Instant.parse("2026-01-01T00:00:00Z")));

        mockMvc.perform(post("/users/me/account").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY)
                        .requestAttr(UserContextInterceptor.USER_REQUEST_ATTR, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@example.com"))
                .andExpect(jsonPath("$.anonymous").value(false));
    }

    @Test
    void convertToAccount_whenAlreadyRegistered_shouldReturnConflict() throws Exception {
        UUID userId = UUID.randomUUID();
        when(convertToAccountUseCase.convertToAccount(userId, "Ana", "ana@example.com", "secret-123"))
                .thenThrow(new AccountAlreadyRegisteredException());

        mockMvc.perform(post("/users/me/account").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY)
                        .requestAttr(UserContextInterceptor.USER_REQUEST_ATTR, userId))
                .andExpect(status().isConflict());
    }

    @Test
    void create_shouldReturnCreatedWithToken() throws Exception {
        UUID userId = UUID.randomUUID();
        when(createUserUseCase.create("Ana", "ana@example.com", "secret-123"))
                .thenReturn(new AuthToken("jwt-value", userId, Instant.parse("2030-01-01T00:00:00Z")));

        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("jwt-value"))
                .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    void create_withInvalidBody_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \",\"email\":\"invalid\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void create_withExistingEmail_shouldReturnConflict() throws Exception {
        when(createUserUseCase.create("Ana", "ana@example.com", "secret-123"))
                .thenThrow(new EmailAlreadyRegisteredException());

        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void me_shouldReturnAuthenticatedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        when(getUserUseCase.getById(userId))
                .thenReturn(User.reconstruct(userId, "Ana", "ana@example.com", Instant.parse("2026-01-01T00:00:00Z")));

        mockMvc.perform(get("/users/me").requestAttr(UserContextInterceptor.USER_REQUEST_ATTR, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.name").value("Ana"))
                .andExpect(jsonPath("$.email").value("ana@example.com"))
                .andExpect(jsonPath("$.anonymous").value(false))
                .andExpect(jsonPath("$.password").doesNotExist());
    }
}
