package com.correai.api.adapter.in.web.badge;

import com.correai.api.domain.port.in.admin.AdminUseCase;
import com.correai.api.domain.port.in.auth.IssueAnonymousTokenUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BadgeTemplateIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired IssueAnonymousTokenUseCase tokens;
    @MockitoBean AdminUseCase admins;

    @Test
    void adminsCreateUsersBrowseAndReceiveSafeSvg() throws Exception {
        var admin = tokens.issue();
        var user = tokens.issue();
        when(admins.isAdmin(admin.userId())).thenReturn(true);
        String a = "Bearer " + admin.token();
        String b = "Bearer " + user.token();
        String body = "{\"name\":\"Steps\",\"description\":\"Walk\",\"icon\":\"FOOTPRINT\",\"color\":\"#12ab34\"}";
        mvc.perform(get("/badge-templates")).andExpect(status().isUnauthorized());
        mvc.perform(post("/badge-templates").header("Authorization", b).contentType(MediaType.APPLICATION_JSON)
                        .content(body)).andExpect(status().isForbidden());
        var response = mvc.perform(post("/badge-templates").header("Authorization", a)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.color").value("#12AB34"))
                .andReturn().getResponse().getContentAsString();
        String id = new tools.jackson.databind.ObjectMapper().readTree(response).get("id").asText();
        mvc.perform(get("/badge-templates/" + id).header("Authorization", b))
                .andExpect(status().isOk()).andExpect(jsonPath("$.icon").value("FOOTPRINT"));
        mvc.perform(get("/badge-templates/" + id + "/image").header("Authorization", b))
                .andExpect(status().isOk()).andExpect(content().contentType("image/svg+xml"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Security-Policy", "default-src 'none'; sandbox"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("#12AB34")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script"))));
        var second = mvc.perform(post("/badge-templates").header("Authorization", a)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"5K\",\"description\":\"Run\",\"icon\":\"FIVE_K\",\"color\":\"#abcdef\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String secondId = new tools.jackson.databind.ObjectMapper().readTree(second).get("id").asText();
        mvc.perform(get("/badge-templates/" + secondId + "/image").header("Authorization", b))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(">5K</text>")));
        mvc.perform(get("/badge-templates").header("Authorization", b).param("size", "101"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/badge-templates/" + UUID.randomUUID()).header("Authorization", b))
                .andExpect(status().isNotFound());
        mvc.perform(post("/badge-templates").header("Authorization", a).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"description\":\"y\",\"icon\":\"FOOTPRINT\",\"color\":\"red\"}"))
                .andExpect(status().isBadRequest());
    }
}
