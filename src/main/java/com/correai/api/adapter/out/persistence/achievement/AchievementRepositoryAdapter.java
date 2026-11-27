package com.correai.api.adapter.out.persistence.achievement;

import com.correai.api.domain.model.achievement.BadgeCode;
import com.correai.api.domain.port.out.achievement.AchievementRepositoryPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

@Component
public class AchievementRepositoryAdapter implements AchievementRepositoryPort {

    private final SpringDataUserBadgeRepository jpaRepository;

    public AchievementRepositoryAdapter(SpringDataUserBadgeRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Map<BadgeCode, Instant> findUnlockedByUserId(UUID userId) {
        Map<BadgeCode, Instant> result = new EnumMap<>(BadgeCode.class);
        for (UserBadgeEntity entity : jpaRepository.findByUserId(userId)) {
            result.put(entity.getBadgeCode(), entity.getUnlockedAt());
        }
        return result;
    }

    @Override
    public boolean grantIfAbsent(UUID userId, BadgeCode code, Instant unlockedAt) {
        if (jpaRepository.existsByUserIdAndBadgeCode(userId, code)) {
            return false;
        }
        try {
            jpaRepository.save(new UserBadgeEntity(null, userId, code, unlockedAt));
            return true;
        } catch (DataIntegrityViolationException e) {
            // Another concurrent evaluation already granted this badge; the unique constraint wins.
            return false;
        }
    }

    @Override
    public void correctUnlockedAt(UUID userId, BadgeCode code, Instant unlockedAt) {
        jpaRepository.findByUserIdAndBadgeCode(userId, code)
                .filter(entity -> unlockedAt.isBefore(entity.getUnlockedAt()))
                .ifPresent(entity -> {
                    entity.setUnlockedAt(unlockedAt);
                    jpaRepository.save(entity);
                });
    }
}
