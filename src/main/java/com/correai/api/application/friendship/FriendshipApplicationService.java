package com.correai.api.application.friendship;

import com.correai.api.domain.model.friendship.FriendshipEntry;
import com.correai.api.domain.model.pagination.PageQuery;
import com.correai.api.domain.model.pagination.PageResult;
import com.correai.api.domain.port.in.friendship.FriendshipsUseCase;
import com.correai.api.domain.port.out.friendship.FriendshipRepositoryPort;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

public class FriendshipApplicationService implements FriendshipsUseCase {
    private final FriendshipRepositoryPort repository;
    private final Clock clock;

    public FriendshipApplicationService(FriendshipRepositoryPort repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    private void validate(UUID userId, UUID otherId) {
        Objects.requireNonNull(userId);
        Objects.requireNonNull(otherId);
        if (userId.equals(otherId)) throw new IllegalArgumentException("Cannot interact with yourself");
    }

    @Override
    public void request(UUID userId, UUID otherId) {
        validate(userId, otherId);
        repository.request(userId, otherId, clock.instant());
    }

    @Override
    public void accept(UUID userId, UUID otherId) {
        validate(userId, otherId);
        repository.accept(userId, otherId);
    }

    @Override
    public void remove(UUID userId, UUID otherId) {
        validate(userId, otherId);
        repository.remove(userId, otherId);
    }

    @Override
    public void block(UUID userId, UUID otherId) {
        validate(userId, otherId);
        repository.block(userId, otherId);
    }

    @Override
    public void unblock(UUID userId, UUID otherId) {
        validate(userId, otherId);
        repository.unblock(userId, otherId);
    }

    @Override
    public PageResult<FriendshipEntry> list(UUID userId, PageQuery page) {
        return repository.list(userId, page);
    }

    @Override
    public PageResult<UUID> listBlocked(UUID userId, PageQuery page) {
        return repository.listBlocked(userId, page);
    }
}
