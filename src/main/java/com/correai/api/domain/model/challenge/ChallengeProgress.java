package com.correai.api.domain.model.challenge;

import com.correai.api.domain.model.activity.Activity;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Snapshot of a participant's progress. Persisting and refreshing it on activity changes is
 * necessary to preserve the value that existed when a challenge ended.
 */
public record ChallengeProgress(double current, Instant achievedAt) {

    public ChallengeProgress {
        if (!Double.isFinite(current) || current < 0) {
            throw new IllegalArgumentException("Challenge progress must be finite and non-negative");
        }
    }

    public static ChallengeProgress empty() {
        return new ChallengeProgress(0, null);
    }

    public ChallengeProgress refresh(Challenge challenge, UUID participantId, List<Activity> activities, Instant now) {
        Objects.requireNonNull(challenge);
        Objects.requireNonNull(participantId);
        Objects.requireNonNull(activities);
        Objects.requireNonNull(now);
        if (challenge.publishedAt() == null || now.isBefore(challenge.publishedAt())) {
            throw new IllegalArgumentException("Challenge must be published");
        }
        if (achievedAt != null || !now.isBefore(challenge.endExclusive())) {
            return this;
        }

        double progress = activities.stream()
                .filter(activity -> qualifies(activity, participantId, challenge, now))
                .mapToDouble(activity -> contribution(activity, challenge.metric()))
                .sum();
        return new ChallengeProgress(progress, progress >= challenge.target() ? now : null);
    }

    private static boolean qualifies(Activity activity, UUID participantId, Challenge challenge, Instant now) {
        Objects.requireNonNull(activity);
        if (!participantId.equals(activity.userId())) {
            throw new IllegalArgumentException("Activity belongs to another user");
        }
        if (activity.createdAt() == null || activity.createdAt().isBefore(challenge.startAt())
                || activity.createdAt().isAfter(now)
                || !activity.createdAt().isBefore(challenge.endExclusive())) {
            return false;
        }
        if (activity.startedAt() != null) {
            return !activity.startedAt().isBefore(challenge.startAt())
                    && activity.startedAt().isBefore(challenge.endExclusive())
                    && !activity.startedAt().isAfter(now);
        }
        return !activity.activityDate().isBefore(challenge.startDate())
                && !activity.activityDate().isAfter(challenge.endDate())
                && !activity.activityDate().isAfter(now.atZone(ZoneId.of(challenge.timeZone())).toLocalDate());
    }

    private static double contribution(Activity activity, ChallengeMetric metric) {
        if (metric == ChallengeMetric.ACTIVITIES) return 1;
        Double distance = activity.distanceKm();
        if (distance == null || !Double.isFinite(distance) || distance <= 0) {
            throw new IllegalArgumentException("Activity distance must be finite and positive");
        }
        return distance;
    }
}
