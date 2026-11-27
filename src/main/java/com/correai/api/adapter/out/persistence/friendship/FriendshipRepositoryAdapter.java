package com.correai.api.adapter.out.persistence.friendship;

import com.correai.api.adapter.out.persistence.user.SpringDataUserRepository;
import com.correai.api.domain.model.friendship.Friendship;
import com.correai.api.domain.model.friendship.FriendshipEntry;
import com.correai.api.domain.model.friendship.FriendshipNotFoundException;
import com.correai.api.domain.model.friendship.FriendshipState;
import com.correai.api.domain.model.pagination.PageQuery;
import com.correai.api.domain.model.pagination.PageResult;
import com.correai.api.domain.port.out.friendship.FriendshipRepositoryPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import java.time.Instant;
import java.util.UUID;

@Component
public class FriendshipRepositoryAdapter implements FriendshipRepositoryPort {
    private final SpringDataUserRepository users;
    private final SpringDataFriendshipRepository friendships;
    private final SpringDataUserBlockRepository blocks;

    public FriendshipRepositoryAdapter(SpringDataUserRepository users, SpringDataFriendshipRepository friendships,
                                       SpringDataUserBlockRepository blocks) {
        this.users = users;
        this.friendships = friendships;
        this.blocks = blocks;
    }

    private void lockUsers(UUID first, UUID second) {
        UUID low = first.compareTo(second) < 0 ? first : second;
        UUID high = first.compareTo(second) < 0 ? second : first;
        users.lockById(low).orElseThrow(FriendshipNotFoundException::new);
        users.lockById(high).orElseThrow(FriendshipNotFoundException::new);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockUsersForInteraction(UUID userId, UUID otherId) {
        if (userId.equals(otherId)) {
            users.lockById(userId).orElseThrow(FriendshipNotFoundException::new);
        } else {
            lockUsers(userId, otherId);
        }
    }

    private java.util.Optional<FriendshipEntity> find(UUID first, UUID second) {
        return first.compareTo(second) < 0
                ? friendships.findByUserLowAndUserHigh(first, second)
                : friendships.findByUserLowAndUserHigh(second, first);
    }

    private void ensureUnblocked(UUID user, UUID other) {
        if (blocks.existsByBlockerIdAndBlockedId(user, other)
                || blocks.existsByBlockerIdAndBlockedId(other, user)) {
            throw new FriendshipNotFoundException();
        }
    }

    @Override
    @Transactional
    public void request(UUID user, UUID other, Instant now) {
        lockUsers(user, other);
        ensureUnblocked(user, other);
        var existing = find(user, other);
        if (existing.isPresent()) {
            existing.get().domain().requestAgain(user);
            return;
        }
        UUID low = user.compareTo(other) < 0 ? user : other;
        UUID high = user.compareTo(other) < 0 ? other : user;
        friendships.save(new FriendshipEntity(low, high, new Friendship(user, other, FriendshipState.PENDING, now)));
    }

    @Override
    @Transactional
    public void accept(UUID user, UUID other) {
        lockUsers(user, other);
        ensureUnblocked(user, other);
        FriendshipEntity entity = find(user, other).orElseThrow(FriendshipNotFoundException::new);
        entity.state = entity.domain().accept(user).state();
    }

    @Override
    @Transactional
    public void remove(UUID user, UUID other) {
        lockUsers(user, other);
        find(user, other).ifPresent(friendships::delete);
    }

    @Override
    @Transactional
    public void block(UUID user, UUID other) {
        lockUsers(user, other);
        find(user, other).ifPresent(friendships::delete);
        if (!blocks.existsByBlockerIdAndBlockedId(user, other)) {
            blocks.save(new UserBlockEntity(user, other));
        }
    }

    @Override
    @Transactional
    public void unblock(UUID user, UUID other) {
        lockUsers(user, other);
        blocks.findByBlockerIdAndBlockedId(user, other).ifPresent(blocks::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<FriendshipEntry> list(UUID user, PageQuery page) {
        var result = friendships.findForUser(user, PageRequest.of(page.page(), page.size()));
        return new PageResult<>(result.getContent().stream().map(f -> new FriendshipEntry(
                f.requesterId.equals(user) ? f.recipientId : f.requesterId,
                f.state, f.recipientId.equals(user), f.createdAt)).toList(),
                page.page(), page.size(), result.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<UUID> listBlocked(UUID user, PageQuery page) {
        var result = blocks.findByBlockerIdOrderByBlockedIdAsc(user, PageRequest.of(page.page(), page.size()));
        return new PageResult<>(result.getContent().stream().map(b -> b.blockedId).toList(),
                page.page(), page.size(), result.getTotalElements());
    }
}
