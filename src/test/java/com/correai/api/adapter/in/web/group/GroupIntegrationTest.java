package com.correai.api.adapter.in.web.group;

import com.correai.api.domain.port.in.auth.IssueAnonymousTokenUseCase;
import com.correai.api.domain.port.in.group.GroupsUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class GroupIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired IssueAnonymousTokenUseCase tokens;
    @Autowired GroupsUseCase groups;

    @Test
    void searchJoinAndLeaveAreAuthenticatedAndIdempotent() throws Exception {
        var owner = tokens.issue();
        var participant = tokens.issue();
        var group = groups.create(owner.userId(), "Group " + UUID.randomUUID());
        String path = "/groups/" + group.id() + "/membership";
        String auth = "Bearer " + participant.token();

        mvc.perform(get("/groups").param("name", group.name())).andExpect(status().isUnauthorized());
        mvc.perform(get("/groups").header("Authorization", auth).param("name", group.name()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].member").value(false));
        mvc.perform(get("/groups").header("Authorization", "Bearer " + owner.token())
                        .param("name", group.name()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].member").value(true));
        mvc.perform(put(path).header("Authorization", auth)).andExpect(status().isNoContent());
        mvc.perform(put(path).header("Authorization", auth)).andExpect(status().isNoContent());
        mvc.perform(get("/groups").header("Authorization", auth).param("name", group.name()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].member").value(true));
        mvc.perform(delete(path).header("Authorization", auth)).andExpect(status().isNoContent());
        mvc.perform(delete(path).header("Authorization", auth)).andExpect(status().isNoContent());
        mvc.perform(get("/groups").header("Authorization", auth).param("name", group.name()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].member").value(false));
        mvc.perform(put("/groups/" + UUID.randomUUID() + "/membership").header("Authorization", auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void validatesCreationAndSearch() throws Exception {
        String auth = "Bearer " + tokens.issue().token();
        mvc.perform(post("/groups").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/groups").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Runners\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.member").value(true));
        mvc.perform(get("/groups").header("Authorization", auth).param("name", " "))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/groups").header("Authorization", auth).param("name", "Runners").param("size", "101"))
                .andExpect(status().isBadRequest());
    }
}
