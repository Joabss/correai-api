package com.correai.api.domain.port.out.friendship;

import com.correai.api.domain.model.friendship.FriendshipEntry;
import com.correai.api.domain.model.pagination.PageQuery;
import com.correai.api.domain.model.pagination.PageResult;
import java.time.Instant;
import java.util.UUID;

public interface FriendshipRepositoryPort {
    void lockUsersForInteraction(UUID userId, UUID otherId);
    void request(UUID userId, UUID otherId, Instant now);
    void accept(UUID userId, UUID otherId);
    void remove(UUID userId, UUID otherId);
    void block(UUID userId, UUID otherId);
    void unblock(UUID userId, UUID otherId);
    PageResult<FriendshipEntry> list(UUID userId, PageQuery page);
    PageResult<UUID> listBlocked(UUID userId, PageQuery page);
}
