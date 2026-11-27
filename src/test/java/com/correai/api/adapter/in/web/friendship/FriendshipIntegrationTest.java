package com.correai.api.adapter.in.web.friendship;

import com.correai.api.domain.port.in.auth.IssueAnonymousTokenUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FriendshipIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired IssueAnonymousTokenUseCase tokens;

    @Test
    void requestsRequireAcceptanceAndCanBeRemoved() throws Exception {
        var first = tokens.issue();
        var second = tokens.issue();
        String a = "Bearer " + first.token();
        String b = "Bearer " + second.token();
        String request = "/friendships/" + second.userId() + "/request";
        mvc.perform(put(request)).andExpect(status().isUnauthorized());
        mvc.perform(put("/friendships/" + first.userId() + "/request").header("Authorization", a))
                .andExpect(status().isBadRequest());
        mvc.perform(put(request).header("Authorization", a)).andExpect(status().isNoContent());
        mvc.perform(put(request).header("Authorization", a)).andExpect(status().isNoContent());
        mvc.perform(put("/friendships/" + first.userId() + "/request").header("Authorization", b))
                .andExpect(status().isConflict());
        mvc.perform(put("/friendships/" + second.userId() + "/accept").header("Authorization", a))
                .andExpect(status().isNotFound());
        mvc.perform(get("/friendships").header("Authorization", b))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].state").value("PENDING"))
                .andExpect(jsonPath("$.content[0].incoming").value(true));
        mvc.perform(put("/friendships/" + first.userId() + "/accept").header("Authorization", b))
                .andExpect(status().isNoContent());
        mvc.perform(put("/friendships/" + first.userId() + "/accept").header("Authorization", b))
                .andExpect(status().isNoContent());
        mvc.perform(get("/friendships").header("Authorization", a))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].state").value("ACCEPTED"));
        mvc.perform(delete("/friendships/" + first.userId()).header("Authorization", b))
                .andExpect(status().isNoContent());
        mvc.perform(get("/friendships").header("Authorization", a))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void blockingRemovesFriendshipPreventsRequestsAndRequiresExplicitUnblock() throws Exception {
        var first = tokens.issue();
        var second = tokens.issue();
        String a = "Bearer " + first.token();
        String b = "Bearer " + second.token();
        mvc.perform(put("/friendships/" + second.userId() + "/request").header("Authorization", a))
                .andExpect(status().isNoContent());
        mvc.perform(put("/friendships/" + first.userId() + "/accept").header("Authorization", b))
                .andExpect(status().isNoContent());
        mvc.perform(put("/friendships/" + second.userId() + "/block").header("Authorization", a))
                .andExpect(status().isNoContent());
        mvc.perform(get("/friendships").header("Authorization", b))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/friendships/blocks").header("Authorization", a))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0]").value(second.userId().toString()));
        mvc.perform(get("/friendships/blocks").header("Authorization", b))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(put("/friendships/" + first.userId() + "/request").header("Authorization", b))
                .andExpect(status().isNotFound());
        mvc.perform(put("/friendships/" + UUID.randomUUID() + "/request").header("Authorization", b))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/friendships/" + second.userId() + "/block").header("Authorization", a))
                .andExpect(status().isNoContent());
        mvc.perform(put("/friendships/" + first.userId() + "/request").header("Authorization", b))
                .andExpect(status().isNoContent());
        mvc.perform(get("/friendships").header("Authorization", a))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].state").value("PENDING"));
    }

    @Test
    void reciprocalBlockRemainsEffectiveWhenOnlyOneUserUnblocks() throws Exception {
        var first = tokens.issue();
        var second = tokens.issue();
        String a = "Bearer " + first.token();
        String b = "Bearer " + second.token();
        mvc.perform(put("/friendships/" + second.userId() + "/block").header("Authorization", a))
                .andExpect(status().isNoContent());
        mvc.perform(put("/friendships/" + first.userId() + "/block").header("Authorization", b))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/friendships/" + second.userId() + "/block").header("Authorization", a))
                .andExpect(status().isNoContent());
        mvc.perform(put("/friendships/" + second.userId() + "/request").header("Authorization", a))
                .andExpect(status().isNotFound());
        mvc.perform(get("/friendships/blocks").header("Authorization", b))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0]").value(first.userId().toString()));
    }
}
