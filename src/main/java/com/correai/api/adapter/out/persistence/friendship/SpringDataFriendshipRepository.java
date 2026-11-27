package com.correai.api.adapter.out.persistence.friendship;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

interface SpringDataFriendshipRepository extends JpaRepository<FriendshipEntity, UUID> {
    Optional<FriendshipEntity> findByUserLowAndUserHigh(UUID low, UUID high);

    @Query("select f from FriendshipEntity f where f.requesterId = :id or f.recipientId = :id "
            + "order by f.createdAt desc, f.id")
    Page<FriendshipEntity> findForUser(@Param("id") UUID id, Pageable page);
}
