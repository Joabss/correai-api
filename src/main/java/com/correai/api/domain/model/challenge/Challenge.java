package com.correai.api.domain.model.challenge;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.DateTimeException;
import java.util.Objects;
import java.util.UUID;

public record Challenge(UUID id, UUID creatorId, String name, String description,
                        ChallengeAudience audience, UUID groupId, ChallengeMetric metric, double target,
                        LocalDate startDate, LocalDate endDate, String timeZone, UUID badgeTemplateId,
                        Instant createdAt, Instant publishedAt) {
    public Challenge {
        Objects.requireNonNull(id);
        Objects.requireNonNull(creatorId);
        Objects.requireNonNull(audience);
        Objects.requireNonNull(metric);
        Objects.requireNonNull(startDate);
        Objects.requireNonNull(endDate);
        Objects.requireNonNull(badgeTemplateId);
        Objects.requireNonNull(createdAt);
        if (name == null || name.isBlank() || name.trim().length() > 100) {
            throw new IllegalArgumentException("Challenge name must contain between 1 and 100 characters");
        }
        if (description == null || description.isBlank() || description.trim().length() > 500) {
            throw new IllegalArgumentException("Challenge description must contain between 1 and 500 characters");
        }
        if ((audience == ChallengeAudience.GROUP) != (groupId != null)) {
            throw new IllegalArgumentException("Group ID is required only for group challenges");
        }
        if (!Double.isFinite(target) || target <= 0
                || (metric == ChallengeMetric.ACTIVITIES && target != Math.floor(target))) {
            throw new IllegalArgumentException("Challenge target must be positive and activity count must be an integer");
        }
        if (endDate.isBefore(startDate) || endDate.equals(LocalDate.MAX)) {
            throw new IllegalArgumentException("Invalid challenge date range");
        }
        try {
            if (timeZone == null || !ZoneId.getAvailableZoneIds().contains(timeZone)) {
                throw new IllegalArgumentException("Time zone must be an IANA region");
            }
            ZoneId.of(timeZone);
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("Time zone must be an IANA region", exception);
        }
        name = name.trim();
        description = description.trim();
    }

    public Instant startAt() {
        return startDate.atStartOfDay(ZoneId.of(timeZone)).toInstant();
    }

    public Instant endExclusive() {
        return endDate.plusDays(1).atStartOfDay(ZoneId.of(timeZone)).toInstant();
    }

    public Challenge publish(Instant now) {
        Objects.requireNonNull(now);
        if (publishedAt != null) throw new ChallengeConflictException("Challenge is already published");
        if (!now.isBefore(endExclusive())) throw new ChallengeConflictException("Challenge has ended");
        return new Challenge(id, creatorId, name, description, audience, groupId, metric, target,
                startDate, endDate, timeZone, badgeTemplateId, createdAt, now);
    }

    public Challenge revise(String newName, String newDescription, ChallengeAudience newAudience, UUID newGroupId,
                            ChallengeMetric newMetric, double newTarget, LocalDate newStart, LocalDate newEnd,
                            String newTimeZone, UUID newBadge) {
        if (publishedAt != null) throw new ChallengeConflictException("Published challenges cannot be edited");
        return new Challenge(id, creatorId, newName, newDescription, newAudience, newGroupId, newMetric, newTarget,
                newStart, newEnd, newTimeZone, newBadge, createdAt, null);
    }
}
