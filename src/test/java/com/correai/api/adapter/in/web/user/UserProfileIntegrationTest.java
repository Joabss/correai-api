package com.correai.api.adapter.in.web.user;

import com.correai.api.domain.model.auth.AuthToken;
import com.correai.api.domain.port.in.user.CreateUserUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserProfileIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CreateUserUseCase users;

    @Test
    void changingEmailRequiresPasswordAndPreservesLogin() throws Exception {
        AuthToken token = users.create("Ana", "profile-test@example.com", "password-123");
        String bearer = "Bearer " + token.token();
        String wrong = "{\"name\":\"Ana Maria\",\"email\":\"updated@example.com\",\"currentPassword\":\"wrong\"}";
        String correct = "{\"name\":\"Ana Maria\",\"email\":\"updated@example.com\","
                + "\"currentPassword\":\"password-123\"}";

        mvc.perform(put("/users/me").header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                .content(wrong)).andExpect(status().isUnauthorized());
        mvc.perform(get("/users/me").header("Authorization", bearer))
                .andExpect(jsonPath("$.email").value("profile-test@example.com"));
        mvc.perform(put("/users/me").header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                .content(correct)).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("updated@example.com"));
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"updated@example.com\",\"password\":\"password-123\"}"))
                .andExpect(status().isOk());
        mvc.perform(put("/users/me").contentType(MediaType.APPLICATION_JSON).content(correct))
                .andExpect(status().isUnauthorized());
    }
}
