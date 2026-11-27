package com.correai.api.domain.port.in.friendship;

import com.correai.api.domain.model.friendship.FriendshipEntry;
import com.correai.api.domain.model.pagination.PageQuery;
import com.correai.api.domain.model.pagination.PageResult;
import java.util.UUID;

public interface FriendshipsUseCase {
    void request(UUID userId, UUID otherId);
    void accept(UUID userId, UUID otherId);
    void remove(UUID userId, UUID otherId);
    void block(UUID userId, UUID otherId);
    void unblock(UUID userId, UUID otherId);
    PageResult<FriendshipEntry> list(UUID userId, PageQuery page);
    PageResult<UUID> listBlocked(UUID userId, PageQuery page);
}
