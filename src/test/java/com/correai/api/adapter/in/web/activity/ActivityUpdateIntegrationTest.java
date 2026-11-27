package com.correai.api.adapter.in.web.activity;

import com.correai.api.domain.model.activity.Activity;
import com.correai.api.domain.model.activity.ActivityType;
import com.correai.api.domain.model.auth.AuthToken;
import com.correai.api.domain.model.media.MediaContent;
import com.correai.api.domain.model.media.MediaKind;
import com.correai.api.domain.port.in.auth.IssueAnonymousTokenUseCase;
import com.correai.api.domain.port.out.activity.ActivityRepositoryPort;
import com.correai.api.domain.port.out.media.MediaRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.time.OffsetDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ActivityUpdateIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired IssueAnonymousTokenUseCase tokens;
    @Autowired ActivityRepositoryPort activities;
    @Autowired MediaRepositoryPort media;

    @Test
    void onlyOwnerCanEditAndFilesAndStartTimeArePreserved() throws Exception {
        AuthToken owner = tokens.issue();
        AuthToken other = tokens.issue();
        Activity saved = activities.save(Activity.createWithStartedAt(owner.userId(), ActivityType.RUN,
                5, 1800, null, null, "before", OffsetDateTime.parse("2026-11-27T22:00:00-03:00")));
        Activity original = activities.findByIdAndUserId(saved.id(), owner.userId()).orElseThrow();
        UUID photoId = UUID.randomUUID();
        byte[] bytes = {1, 2, 3};
        media.saveActivityPhoto(owner.userId(), original.id(), photoId, new MediaContent("image/png", bytes));
        media.save(owner.userId(), original.id(), MediaKind.ACTIVITY_ROUTE,
                new MediaContent("application/gpx+xml", bytes));
        String path = "/activities/" + original.id();
        String payload = "{\"type\":\"WALK\",\"distanceKm\":2,\"durationSeconds\":1200,\"notes\":\"after\"}";
        mvc.perform(put(path).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isUnauthorized());
        mvc.perform(put(path).header("Authorization", "Bearer " + other.token())
                .contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isNotFound());
        assertEquals(original, activities.findByIdAndUserId(original.id(), owner.userId()).orElseThrow());
        mvc.perform(put(path).header("Authorization", "Bearer " + owner.token())
                .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk()).andExpect(jsonPath("$.avgPace").value("10:00"))
                .andExpect(jsonPath("$.notes").value("after"));
        Activity updated = activities.findByIdAndUserId(original.id(), owner.userId()).orElseThrow();
        assertEquals(original.id(), updated.id());
        assertEquals(original.createdAt(), updated.createdAt());
        assertEquals(original.activityDate(), updated.activityDate());
        assertEquals(original.startTime(), updated.startTime());
        assertEquals(original.startedAt(), updated.startedAt());
        assertEquals(2.0, activities.findLongestDistance(owner.userId()));
        assertArrayEquals(bytes, media.findActivityPhoto(owner.userId(), original.id(), photoId).orElseThrow().bytes());
        assertArrayEquals(bytes, media.find(owner.userId(), original.id(), MediaKind.ACTIVITY_ROUTE).orElseThrow().bytes());
        mvc.perform(get("/activities").header("Authorization", "Bearer " + owner.token()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].distanceKm").value(2));
    }
}
