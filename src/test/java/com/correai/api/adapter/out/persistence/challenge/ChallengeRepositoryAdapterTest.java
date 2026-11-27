package com.correai.api.adapter.out.persistence.challenge;

import com.correai.api.domain.model.challenge.Challenge;
import com.correai.api.domain.model.challenge.ChallengeAudience;
import com.correai.api.domain.model.challenge.ChallengeConflictException;
import com.correai.api.domain.model.challenge.ChallengeMetric;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChallengeRepositoryAdapterTest {
    private final SpringDataChallengeRepository repository = mock(SpringDataChallengeRepository.class);
    private final ChallengeRepositoryAdapter adapter = new ChallengeRepositoryAdapter(repository);
    private final Challenge draft = new Challenge(UUID.randomUUID(), UUID.randomUUID(), "Walk", "Description",
            ChallengeAudience.FRIENDS, null, ChallengeMetric.DISTANCE_KM, 10,
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), "America/Sao_Paulo",
            UUID.randomUUID(), Instant.parse("2026-05-01T12:00:00Z"), null);

    private Challenge rename(Challenge original, String name) {
        return original.revise(name, original.description(), original.audience(), original.groupId(),
                original.metric(), original.target(), original.startDate(), original.endDate(),
                original.timeZone(), original.badgeTemplateId());
    }

    @Test
    void updatesUnchangedDraft() {
        ChallengeEntity entity = new ChallengeEntity(draft);
        when(repository.lockOwn(draft.id(), draft.creatorId())).thenReturn(Optional.of(entity));
        Challenge revised = rename(draft, "Revised");
        assertEquals(revised, adapter.saveDraft(draft, revised));
        assertEquals(revised, entity.domain());
    }

    @Test
    void rejectsStaleDraftWithoutOverwritingNewerChanges() {
        Challenge newer = rename(draft, "Other edit");
        ChallengeEntity entity = new ChallengeEntity(newer);
        when(repository.lockOwn(draft.id(), draft.creatorId())).thenReturn(Optional.of(entity));
        assertThrows(ChallengeConflictException.class, () -> adapter.saveDraft(draft, rename(draft, "Old edit")));
        assertEquals(newer, entity.domain());
    }

    @Test
    void rejectsEditAfterPublication() {
        Challenge published = draft.publish(Instant.parse("2026-06-15T12:00:00Z"));
        ChallengeEntity entity = new ChallengeEntity(published);
        when(repository.lockOwn(draft.id(), draft.creatorId())).thenReturn(Optional.of(entity));
        assertThrows(ChallengeConflictException.class, () -> adapter.saveDraft(draft, rename(draft, "Old edit")));
        assertEquals(published, entity.domain());
    }

    @Test
    void rejectsRevisionForAnotherChallenge() {
        Challenge other = new Challenge(UUID.randomUUID(), draft.creatorId(), draft.name(), draft.description(),
                draft.audience(), draft.groupId(), draft.metric(), draft.target(), draft.startDate(),
                draft.endDate(), draft.timeZone(), draft.badgeTemplateId(), draft.createdAt(), null);
        assertThrows(IllegalArgumentException.class, () -> adapter.saveDraft(draft, other));
        verifyNoInteractions(repository);
    }
}
