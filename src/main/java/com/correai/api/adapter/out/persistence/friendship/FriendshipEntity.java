package com.correai.api.adapter.out.persistence.friendship;

import com.correai.api.domain.model.friendship.Friendship;
import com.correai.api.domain.model.friendship.FriendshipState;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "friendships", uniqueConstraints = @UniqueConstraint(
        name = "uk_friendship_pair", columnNames = {"user_low", "user_high"}))
class FriendshipEntity {
    @Id UUID id;
    @Column(nullable = false) UUID userLow;
    @Column(nullable = false) UUID userHigh;
    @Column(nullable = false) UUID requesterId;
    @Column(nullable = false) UUID recipientId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) FriendshipState state;
    @Column(nullable = false) Instant createdAt;

    protected FriendshipEntity() {}

    FriendshipEntity(UUID low, UUID high, Friendship friendship) {
        id = UUID.randomUUID();
        userLow = low;
        userHigh = high;
        requesterId = friendship.requesterId();
        recipientId = friendship.recipientId();
        state = friendship.state();
        createdAt = friendship.createdAt();
    }

    Friendship domain() {
        return new Friendship(requesterId, recipientId, state, createdAt);
    }
}
