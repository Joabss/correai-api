package com.correai.api.adapter.out.persistence.friendship;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

interface SpringDataUserBlockRepository extends JpaRepository<UserBlockEntity, UUID> {
    boolean existsByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);
    Optional<UserBlockEntity> findByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);
    Page<UserBlockEntity> findByBlockerIdOrderByBlockedIdAsc(UUID blockerId, Pageable page);
}
