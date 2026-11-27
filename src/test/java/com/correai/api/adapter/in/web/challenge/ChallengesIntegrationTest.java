package com.correai.api.adapter.in.web.challenge;

import com.correai.api.domain.model.auth.AuthToken;
import com.correai.api.domain.model.badge.BadgeIcon;
import com.correai.api.domain.port.in.admin.AdminUseCase;
import com.correai.api.domain.port.in.auth.IssueAnonymousTokenUseCase;
import com.correai.api.domain.port.in.badge.BadgeTemplatesUseCase;
import com.correai.api.domain.port.in.friendship.FriendshipsUseCase;
import com.correai.api.domain.port.in.group.GroupsUseCase;
import com.correai.api.domain.port.out.admin.AdminRepositoryPort;
import com.correai.api.domain.port.out.activity.ActivityRepositoryPort;
import com.correai.api.domain.port.out.challenge.ChallengeParticipationRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ChallengesIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired IssueAnonymousTokenUseCase tokens;
    @Autowired BadgeTemplatesUseCase badges;
    @Autowired FriendshipsUseCase friendships;
    @Autowired GroupsUseCase groups;
    @MockitoBean AdminUseCase admins;
    @MockitoBean AdminRepositoryPort adminRepository;
    @MockitoSpyBean ChallengeParticipationRepositoryPort participations;
    @Autowired ActivityRepositoryPort activityRepository;

    private String auth(AuthToken user) {
        return "Bearer " + user.token();
    }

    private UUID badge(AuthToken owner) {
        when(admins.isAdmin(owner.userId())).thenReturn(true);
        return badges.create(owner.userId(), "Steps", "Walk", BadgeIcon.FOOTPRINT, "#123456").id();
    }

    private String draftBody(UUID badge, String audience, UUID group, double target,
                             LocalDate start, LocalDate end) {
        String groupField = group == null ? "" : ",\"groupId\":\"" + group + "\"";
        return """
                {"name":"My challenge","description":"Run","audience":"%s"%s,"metric":"DISTANCE_KM",
                 "target":%s,"startDate":"%s","endDate":"%s",
                 "timeZone":"America/Sao_Paulo","badgeTemplateId":"%s"}
                """.formatted(audience, groupField, target, start, end, badge);
    }

    private String createAndPublish(AuthToken owner, UUID badge, String audience, UUID group, double target)
            throws Exception {
        LocalDate today = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        String body = draftBody(badge, audience, group, target, today.minusDays(1), today.plusDays(1));
        String json = mvc.perform(post("/challenge-drafts").header("Authorization", auth(owner))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = new tools.jackson.databind.ObjectMapper().readTree(json).get("id").asText();
        mvc.perform(post("/challenge-drafts/" + id + "/publish").header("Authorization", auth(owner)))
                .andExpect(status().isOk());
        return id;
    }

    @Test
    void friendJoinsWithEarlierActivityAndKeepsBadgeAfterFriendshipRemoval() throws Exception {
        var owner = tokens.issue();
        var friend = tokens.issue();
        var stranger = tokens.issue();
        UUID badge = badge(owner);
        friendships.request(owner.userId(), friend.userId());
        String id = createAndPublish(owner, badge, "FRIENDS", null, 2);
        mvc.perform(get("/challenges")).andExpect(status().isUnauthorized());
        mvc.perform(get("/challenge-badges/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/challenges/" + id).header("Authorization", auth(friend)))
                .andExpect(status().isNotFound());
        friendships.accept(friend.userId(), owner.userId());
        mvc.perform(get("/challenges").header("Authorization", auth(friend)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[*].id", hasItem(id)));
        mvc.perform(put("/challenges/" + id + "/participation").header("Authorization", auth(stranger)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/activities").header("Authorization", auth(friend)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"WALK\",\"distanceKm\":2,\"durationSeconds\":1800}"))
                .andExpect(status().isOk());
        mvc.perform(put("/challenges/" + id + "/participation").header("Authorization", auth(friend)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.participation.progress.current").value(2))
                .andExpect(jsonPath("$.participation.progress.achievedAt").exists());
        mvc.perform(put("/challenges/" + id + "/participation").header("Authorization", auth(friend)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.participation.progress.current").value(2));
        friendships.remove(owner.userId(), friend.userId());
        mvc.perform(get("/challenges/" + id).header("Authorization", auth(friend))).andExpect(status().isNotFound());
        mvc.perform(get("/challenges").header("Authorization", auth(friend)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[*].id", not(hasItem(id))));
        mvc.perform(get("/challenge-badges/me").header("Authorization", auth(friend)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].badge.id").value(badge.toString()));
        mvc.perform(get("/challenge-badges/me").header("Authorization", auth(stranger)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void groupVisibilityRequiresMembershipAndRespectsBlocks() throws Exception {
        var owner = tokens.issue();
        var member = tokens.issue();
        UUID badge = badge(owner);
        var group = groups.create(owner.userId(), "Runners");
        String id = createAndPublish(owner, badge, "GROUP", group.id(), 10);
        mvc.perform(get("/challenges/" + id).header("Authorization", auth(member)))
                .andExpect(status().isNotFound());
        groups.join(member.userId(), group.id());
        mvc.perform(get("/challenges/" + id).header("Authorization", auth(member)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.participation").doesNotExist());
        mvc.perform(put("/challenges/" + id + "/participation").header("Authorization", auth(member)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.participation.progress.current").value(0));
        mvc.perform(post("/activities").header("Authorization", auth(member)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"RUN\",\"distanceKm\":10,\"durationSeconds\":3600}"))
                .andExpect(status().isOk());
        friendships.block(owner.userId(), member.userId());
        mvc.perform(get("/challenges/" + id).header("Authorization", auth(member)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/challenge-badges/me").header("Authorization", auth(member)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].badge.id").value(badge.toString()));
        friendships.unblock(owner.userId(), member.userId());
        groups.leave(member.userId(), group.id());
        mvc.perform(get("/challenges/" + id).header("Authorization", auth(member)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/challenge-badges/me").header("Authorization", auth(member)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/challenges/" + id).header("Authorization", auth(owner)))
                .andExpect(status().isOk());
    }

    @Test
    void globalPublicationRechecksAdministratorAndBlocksHideChallenges() throws Exception {
        var owner = tokens.issue();
        var member = tokens.issue();
        UUID badge = badge(owner);
        LocalDate today = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        String body = draftBody(badge, "GLOBAL", null, 1, today.minusDays(1), today.plusDays(1));
        String json = mvc.perform(post("/challenge-drafts").header("Authorization", auth(owner))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = new tools.jackson.databind.ObjectMapper().readTree(json).get("id").asText();
        mvc.perform(post("/challenge-drafts/" + id + "/publish").header("Authorization", auth(owner)))
                .andExpect(status().isForbidden());
        when(adminRepository.isAdminLocked(owner.userId())).thenReturn(true);
        mvc.perform(post("/challenge-drafts/" + id + "/publish").header("Authorization", auth(owner)))
                .andExpect(status().isOk());
        mvc.perform(get("/challenges/" + id).header("Authorization", auth(member)))
                .andExpect(status().isOk());
        friendships.block(member.userId(), owner.userId());
        mvc.perform(get("/challenges/" + id).header("Authorization", auth(member)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/challenges").header("Authorization", auth(member)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[*].id", not(hasItem(id))));
    }

    @Test
    void participationUpdateFailureRollsBackActivity() throws Exception {
        var owner = tokens.issue();
        var friend = tokens.issue();
        UUID badge = badge(owner);
        friendships.request(owner.userId(), friend.userId());
        friendships.accept(friend.userId(), owner.userId());
        String id = createAndPublish(owner, badge, "FRIENDS", null, 1);
        mvc.perform(put("/challenges/" + id + "/participation").header("Authorization", auth(friend)))
                .andExpect(status().isOk());
        doThrow(new IllegalStateException("Simulated challenge update failure"))
                .when(participations).update(any(), any());
        mvc.perform(post("/activities").header("Authorization", auth(friend)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"WALK\",\"distanceKm\":2,\"durationSeconds\":1800}"))
                .andExpect(status().isInternalServerError());
        assertTrue(activityRepository.findAllByUserId(friend.userId()).isEmpty());
    }

    @Test
    void activityEditsAndDeletionUpdateProgressAndKeepAward() throws Exception {
        var owner = tokens.issue();
        var friend = tokens.issue();
        UUID badge = badge(owner);
        friendships.request(owner.userId(), friend.userId());
        friendships.accept(friend.userId(), owner.userId());
        String id = createAndPublish(owner, badge, "FRIENDS", null, 10);
        mvc.perform(put("/challenges/" + id + "/participation").header("Authorization", auth(friend)))
                .andExpect(status().isOk());
        String response = mvc.perform(post("/activities").header("Authorization", auth(friend))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"WALK\",\"distanceKm\":4,\"durationSeconds\":1800}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String activity = new tools.jackson.databind.ObjectMapper().readTree(response).get("id").asText();
        mvc.perform(get("/challenges/" + id).header("Authorization", auth(friend)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.participation.progress.current").value(4));
        mvc.perform(put("/activities/" + activity).header("Authorization", auth(friend))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"WALK\",\"distanceKm\":1,\"durationSeconds\":1800}"))
                .andExpect(status().isOk());
        mvc.perform(get("/challenges/" + id).header("Authorization", auth(friend)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.participation.progress.current").value(1));
        mvc.perform(put("/activities/" + activity).header("Authorization", auth(friend))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"WALK\",\"distanceKm\":10,\"durationSeconds\":1800}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/activities/" + activity).header("Authorization", auth(friend)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/challenges/" + id).header("Authorization", auth(friend)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.participation.progress.current").value(10))
                .andExpect(jsonPath("$.participation.progress.achievedAt").exists());
        mvc.perform(get("/challenge-badges/me").header("Authorization", auth(friend)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].badge.id").value(badge.toString()));
    }

    @Test
    void futureChallengeCannotBeJoinedUntilItsStart() throws Exception {
        var owner = tokens.issue();
        var friend = tokens.issue();
        UUID badge = badge(owner);
        friendships.request(owner.userId(), friend.userId());
        friendships.accept(friend.userId(), owner.userId());
        LocalDate today = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        String body = draftBody(badge, "FRIENDS", null, 2, today.plusDays(2), today.plusDays(3));
        String json = mvc.perform(post("/challenge-drafts").header("Authorization", auth(owner))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = new tools.jackson.databind.ObjectMapper().readTree(json).get("id").asText();
        mvc.perform(post("/challenge-drafts/" + id + "/publish").header("Authorization", auth(owner)))
                .andExpect(status().isOk());
        mvc.perform(put("/challenges/" + id + "/participation").header("Authorization", auth(friend)))
                .andExpect(status().isConflict());
    }

    @Test
    void groupPublicationFailsIfCreatorLeftAfterSavingDraft() throws Exception {
        var owner = tokens.issue();
        UUID badge = badge(owner);
        var group = groups.create(owner.userId(), "Runners");
        LocalDate today = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        String body = draftBody(badge, "GROUP", group.id(), 2, today.minusDays(1), today.plusDays(1));
        String json = mvc.perform(post("/challenge-drafts").header("Authorization", auth(owner))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = new tools.jackson.databind.ObjectMapper().readTree(json).get("id").asText();
        groups.leave(owner.userId(), group.id());
        mvc.perform(post("/challenge-drafts/" + id + "/publish").header("Authorization", auth(owner)))
                .andExpect(status().isNotFound());
    }

    @Test
    void visibleChallengesArePaginatedBeforeTheyAreReturned() throws Exception {
        var owner = tokens.issue();
        var friend = tokens.issue();
        var stranger = tokens.issue();
        UUID badge = badge(owner);
        friendships.request(owner.userId(), friend.userId());
        friendships.accept(friend.userId(), owner.userId());
        var expected = java.util.Set.of(createAndPublish(owner, badge, "FRIENDS", null, 2),
                createAndPublish(owner, badge, "FRIENDS", null, 3),
                createAndPublish(owner, badge, "FRIENDS", null, 4));
        String first = mvc.perform(get("/challenges?page=0&size=1").header("Authorization", auth(friend)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
                .andReturn().getResponse().getContentAsString();
        var mapper = new tools.jackson.databind.ObjectMapper();
        var page = mapper.readTree(first);
        int total = page.get("totalElements").asInt();
        org.junit.jupiter.api.Assertions.assertTrue(total >= 3);
        org.junit.jupiter.api.Assertions.assertEquals(total, page.get("totalPages").asInt());
        var ids = new HashSet<String>();
        ids.add(page.get("content").get(0).get("id").asText());
        for (int index = 1; index < total; index++) {
            String json = mvc.perform(get("/challenges?page=" + index + "&size=1")
                            .header("Authorization", auth(friend)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
                    .andReturn().getResponse().getContentAsString();
            ids.add(mapper.readTree(json).get("content").get(0).get("id").asText());
        }
        org.junit.jupiter.api.Assertions.assertEquals(total, ids.size());
        org.junit.jupiter.api.Assertions.assertTrue(ids.containsAll(expected));
        for (String id : expected) {
            mvc.perform(get("/challenges/" + id).header("Authorization", auth(stranger)))
                    .andExpect(status().isNotFound());
        }
    }
}
