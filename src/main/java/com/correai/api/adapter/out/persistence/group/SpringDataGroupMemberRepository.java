package com.correai.api.adapter.out.persistence.group;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

interface SpringDataGroupMemberRepository extends JpaRepository<GroupMemberEntity, UUID> {
    boolean existsByGroupIdAndUserId(UUID groupId, UUID userId);
    Optional<GroupMemberEntity> findByGroupIdAndUserId(UUID groupId, UUID userId);
}
