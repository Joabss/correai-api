package com.correai.api.adapter.out.persistence.challenge;

import com.correai.api.domain.model.challenge.Challenge;
import com.correai.api.domain.model.challenge.ChallengeAudience;
import com.correai.api.domain.model.challenge.ChallengeMetric;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "shared_challenges")
class ChallengeEntity {
    @Id UUID id;
    @Column(nullable = false) UUID creatorId;
    @Column(nullable = false, length = 100) String name;
    @Column(nullable = false, length = 500) String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) ChallengeAudience audience;
    UUID groupId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) ChallengeMetric metric;
    @Column(nullable = false) double target;
    @Column(nullable = false) LocalDate startDate;
    @Column(nullable = false) LocalDate endDate;
    @Column(nullable = false, length = 100) String timeZone;
    @Column(nullable = false) UUID badgeTemplateId;
    @Column(nullable = false) Instant createdAt;
    Instant publishedAt;

    protected ChallengeEntity() {}

    ChallengeEntity(Challenge c) {
        id = c.id();
        creatorId = c.creatorId();
        apply(c);
    }

    void apply(Challenge c) {
        name = c.name();
        description = c.description();
        audience = c.audience();
        groupId = c.groupId();
        metric = c.metric();
        target = c.target();
        startDate = c.startDate();
        endDate = c.endDate();
        timeZone = c.timeZone();
        badgeTemplateId = c.badgeTemplateId();
        createdAt = c.createdAt();
        publishedAt = c.publishedAt();
    }

    Challenge domain() {
        return new Challenge(id, creatorId, name, description, audience, groupId, metric, target,
                startDate, endDate, timeZone, badgeTemplateId, createdAt, publishedAt);
    }
}
