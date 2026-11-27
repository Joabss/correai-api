package com.correai.api.domain.model.friendship;

import java.time.Instant;
import java.util.UUID;

public record FriendshipEntry(UUID userId, FriendshipState state, boolean incoming, Instant createdAt) {}
