package com.correai.api.adapter.out.persistence.achievement;

import com.correai.api.domain.model.achievement.BadgeCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataUserBadgeRepository extends JpaRepository<UserBadgeEntity, UUID> {

    List<UserBadgeEntity> findByUserId(UUID userId);

    boolean existsByUserIdAndBadgeCode(UUID userId, BadgeCode badgeCode);

    Optional<UserBadgeEntity> findByUserIdAndBadgeCode(UUID userId, BadgeCode badgeCode);
}
