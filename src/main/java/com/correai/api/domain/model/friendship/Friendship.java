package com.correai.api.domain.model.friendship;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Friendship(UUID requesterId, UUID recipientId, FriendshipState state, Instant createdAt) {
    public Friendship {
        Objects.requireNonNull(requesterId);
        Objects.requireNonNull(recipientId);
        Objects.requireNonNull(state);
        Objects.requireNonNull(createdAt);
        if (requesterId.equals(recipientId)) {
            throw new IllegalArgumentException("Cannot befriend yourself");
        }
    }

    public Friendship accept(UUID userId) {
        if (!recipientId.equals(userId)) {
            throw new FriendshipNotFoundException();
        }
        return state == FriendshipState.ACCEPTED ? this
                : new Friendship(requesterId, recipientId, FriendshipState.ACCEPTED, createdAt);
    }

    public Friendship requestAgain(UUID userId) {
        if (state == FriendshipState.ACCEPTED || requesterId.equals(userId)) {
            return this;
        }
        throw new FriendshipConflictException();
    }
}
