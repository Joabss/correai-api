package com.correai.api.adapter.in.web.admin;

import com.correai.api.domain.port.in.auth.IssueAnonymousTokenUseCase;
import com.correai.api.domain.port.in.user.CreateUserUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.security.admin-bootstrap-secret=test-bootstrap-secret-32-bytes-long")
@AutoConfigureMockMvc
class AdminIntegrationTest {
    private static final String SECRET = "test-bootstrap-secret-32-bytes-long";
    @Autowired MockMvc mvc;
    @Autowired IssueAnonymousTokenUseCase anonymous;
    @Autowired CreateUserUseCase users;
    @Autowired JdbcTemplate jdbc;

    @Test
    void bootstrapGrantRevocationAndAuditEnforceAuthorization() throws Exception {
        jdbc.update("insert into admin_state (id, initialized) values (1, false)");
        var first = users.create("First", UUID.randomUUID() + "@example.test", "password123");
        var second = users.create("Second", UUID.randomUUID() + "@example.test", "password123");
        var anon = anonymous.issue();
        String a = "Bearer " + first.token();
        String b = "Bearer " + second.token();
        String c = "Bearer " + anon.token();
        mvc.perform(post("/admin/bootstrap").header("X-Admin-Bootstrap-Secret", SECRET))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/admin/bootstrap").header("Authorization", a))
                .andExpect(status().isForbidden());
        mvc.perform(post("/admin/bootstrap").header("Authorization", a).header("X-Admin-Bootstrap-Secret", "invalid"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/admin/bootstrap").header("Authorization", c).header("X-Admin-Bootstrap-Secret", SECRET))
                .andExpect(status().isConflict());
        mvc.perform(put("/admin/users/" + second.userId()).header("Authorization", a))
                .andExpect(status().isForbidden());
        mvc.perform(post("/admin/bootstrap").header("Authorization", a).header("X-Admin-Bootstrap-Secret", SECRET))
                .andExpect(status().isNoContent());
        mvc.perform(post("/admin/bootstrap").header("Authorization", b).header("X-Admin-Bootstrap-Secret", SECRET))
                .andExpect(status().isConflict());
        mvc.perform(get("/admin/role").header("Authorization", a))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN"));
        mvc.perform(put("/admin/users/" + anon.userId()).header("Authorization", a))
                .andExpect(status().isConflict());
        mvc.perform(delete("/admin/users/" + first.userId()).header("Authorization", a))
                .andExpect(status().isConflict());
        mvc.perform(put("/admin/users/" + second.userId()).header("Authorization", a))
                .andExpect(status().isNoContent());
        String badge = "{\"name\":\"Steps\",\"description\":\"Walk\",\"icon\":\"FIVE_K\",\"color\":\"#112233\"}";
        mvc.perform(post("/badge-templates").header("Authorization", b).contentType(MediaType.APPLICATION_JSON)
                        .content(badge)).andExpect(status().isCreated());
        mvc.perform(get("/admin/role").header("Authorization", b))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN"));
        mvc.perform(delete("/admin/users/" + second.userId()).header("Authorization", a))
                .andExpect(status().isNoContent());
        mvc.perform(put("/admin/users/" + second.userId()).header("Authorization", b))
                .andExpect(status().isForbidden());
        mvc.perform(post("/badge-templates").header("Authorization", b).contentType(MediaType.APPLICATION_JSON)
                        .content(badge)).andExpect(status().isForbidden());
        mvc.perform(get("/admin/role").header("Authorization", b))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("USER"));
        assertEquals(3, jdbc.queryForObject("select count(*) from admin_audit", Integer.class));
    }
}
