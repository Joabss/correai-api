package com.correai.api.adapter.in.web.achievement.dto;

import com.correai.api.domain.model.achievement.Achievement;

import java.time.Instant;

public record AchievementResponse(
        String code,
        String name,
        String description,
        boolean unlocked,
        Instant unlockedAt
) {
    public static AchievementResponse from(Achievement achievement) {
        return new AchievementResponse(
                achievement.code().code(),
                achievement.code().displayName(),
                achievement.code().description(),
                achievement.unlocked(),
                achievement.unlockedAt());
    }
}
