package com.correai.api.adapter.in.web.stats;

import com.correai.api.domain.model.activity.Activity;
import com.correai.api.domain.model.activity.ActivityType;
import com.correai.api.domain.model.auth.AuthToken;
import com.correai.api.domain.model.media.MediaKind;
import com.correai.api.domain.port.in.auth.IssueAnonymousTokenUseCase;
import com.correai.api.domain.port.in.media.MediaUseCase;
import com.correai.api.domain.port.out.activity.ActivityRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PersonalRecordIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired IssueAnonymousTokenUseCase tokens;
    @Autowired ActivityRepositoryPort activities;
    @Autowired MediaUseCase media;

    @Test
    void ranksTimedWalkingRoutesPrivatelyAndRemovesDeletedEfforts() throws Exception {
        AuthToken owner = tokens.issue();
        AuthToken other = tokens.issue();
        var activity = activities.save(Activity.create(owner.userId(), ActivityType.WALK,
                8, 6000, null, null, null, LocalDate.now()));
        String xml = """
                <gpx xmlns="http://www.topografix.com/GPX/1/1"><trk><trkseg>
                <trkpt lat="0" lon="0"><time>2026-11-27T22:00:00-03:00</time></trkpt>
                <trkpt lat="0" lon="0.05"><time>2026-11-28T01:30:00Z</time></trkpt>
                </trkseg></trk></gpx>
                """;
        media.save(owner.userId(), activity.id(), MediaKind.ACTIVITY_ROUTE, xml.getBytes(StandardCharsets.UTF_8));
        mvc.perform(get("/stats/records")).andExpect(status().isUnauthorized());
        mvc.perform(get("/stats/records").header("Authorization", "Bearer " + other.token()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].efforts.length()").value(0));
        String auth = "Bearer " + owner.token();
        mvc.perform(get("/stats/records").header("Authorization", auth)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].distanceKm").value(5.0))
                .andExpect(jsonPath("$[0].efforts[0].activityId").value(activity.id().toString()))
                .andExpect(jsonPath("$[1].efforts.length()").value(0))
                .andExpect(jsonPath("$[2].distanceKm").value(21.0975))
                .andExpect(jsonPath("$[3].distanceKm").value(42.195));
        mvc.perform(put("/activities/" + activity.id()).header("Authorization", auth)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"RUN\",\"distanceKm\":1,\"durationSeconds\":300}"))
                .andExpect(status().isOk());
        mvc.perform(get("/stats/records").header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].efforts.length()").value(1));
        mvc.perform(delete("/activities/" + activity.id()).header("Authorization", auth))
                .andExpect(status().isNoContent());
        mvc.perform(get("/stats/records").header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].efforts.length()").value(0));
    }
}
