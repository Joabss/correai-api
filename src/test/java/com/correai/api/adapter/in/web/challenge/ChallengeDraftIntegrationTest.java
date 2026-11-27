package com.correai.api.adapter.in.web.challenge;

import com.correai.api.domain.model.badge.BadgeIcon;
import com.correai.api.domain.port.in.admin.AdminUseCase;
import com.correai.api.domain.port.in.auth.IssueAnonymousTokenUseCase;
import com.correai.api.domain.port.in.badge.BadgeTemplatesUseCase;
import com.correai.api.domain.port.in.group.GroupsUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ChallengeDraftIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired IssueAnonymousTokenUseCase tokens;
    @Autowired BadgeTemplatesUseCase badges;
    @Autowired GroupsUseCase groups;
    @MockitoBean AdminUseCase admins;

    @Test
    void draftsArePrivateAndPermissionChecked() throws Exception {
        var owner = tokens.issue();
        var other = tokens.issue();
        when(admins.isAdmin(owner.userId())).thenReturn(true);
        var badge = badges.create(owner.userId(), "Steps", "Walk", BadgeIcon.FOOTPRINT, "#123456");
        when(admins.isAdmin(owner.userId())).thenReturn(false);
        String auth = "Bearer " + owner.token();
        String otherAuth = "Bearer " + other.token();
        String body = """
                {"name":"My run","description":"Run","audience":"FRIENDS","metric":"DISTANCE_KM",
                 "target":50,"startDate":"%s","endDate":"%s",
                 "timeZone":"America/Sao_Paulo","badgeTemplateId":"%s"}
                """.formatted(LocalDate.now(ZoneId.of("America/Sao_Paulo")).minusDays(1),
                LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusDays(1), badge.id());
        mvc.perform(post("/challenge-drafts").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        String response = mvc.perform(post("/challenge-drafts").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.publishedAt").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String id = new tools.jackson.databind.ObjectMapper().readTree(response).get("id").asText();
        mvc.perform(get("/challenge-drafts/" + id).header("Authorization", otherAuth))
                .andExpect(status().isNotFound());
        mvc.perform(put("/challenge-drafts/" + id).header("Authorization", otherAuth)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
        mvc.perform(put("/challenge-drafts/" + id).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body.replace("My run", "Edited")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Edited"));
        mvc.perform(get("/challenge-drafts").header("Authorization", otherAuth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(post("/challenge-drafts/" + id + "/publish").header("Authorization", otherAuth))
                .andExpect(status().isNotFound());
        mvc.perform(post("/challenge-drafts/" + id + "/publish").header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.publishedAt").exists());
        mvc.perform(get("/challenge-drafts").header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(post("/challenge-drafts/" + id + "/publish").header("Authorization", auth))
                .andExpect(status().isConflict());
        mvc.perform(put("/challenge-drafts/" + id).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        mvc.perform(post("/challenge-drafts").header("Authorization", otherAuth)
                        .contentType(MediaType.APPLICATION_JSON).content(body.replace("FRIENDS", "GLOBAL")))
                .andExpect(status().isForbidden());
        var group = groups.create(owner.userId(), "Runners");
        String groupBody = body.replace("\"FRIENDS\"", "\"GROUP\",\"groupId\":\"" + group.id() + "\"");
        mvc.perform(post("/challenge-drafts").header("Authorization", otherAuth)
                        .contentType(MediaType.APPLICATION_JSON).content(groupBody))
                .andExpect(status().isNotFound());
        mvc.perform(post("/challenge-drafts").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(groupBody))
                .andExpect(status().isCreated());
        mvc.perform(post("/challenge-drafts").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body.replace(badge.id().toString(), UUID.randomUUID().toString())))
                .andExpect(status().isNotFound());
    }
}
