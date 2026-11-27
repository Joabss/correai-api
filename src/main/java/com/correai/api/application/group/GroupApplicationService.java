package com.correai.api.application.group;

import com.correai.api.domain.model.group.Group;
import com.correai.api.domain.model.pagination.PageQuery;
import com.correai.api.domain.model.pagination.PageResult;
import com.correai.api.domain.port.in.group.GroupsUseCase;
import com.correai.api.domain.port.out.group.GroupRepositoryPort;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

public class GroupApplicationService implements GroupsUseCase {
    private final GroupRepositoryPort groups;
    private final Clock clock;

    public GroupApplicationService(GroupRepositoryPort groups, Clock clock) {
        this.groups = groups;
        this.clock = clock;
    }

    @Override
    public Group create(UUID userId, String name) {
        return groups.create(new Group(UUID.randomUUID(), name, userId, clock.instant(), true));
    }

    @Override
    public PageResult<Group> search(UUID userId, String name, PageQuery page) {
        Objects.requireNonNull(userId);
        if (name == null || name.isBlank() || name.trim().length() > 100) {
            throw new IllegalArgumentException("Search must contain between 1 and 100 characters");
        }
        return groups.search(userId, name.trim(), page);
    }

    @Override
    public void join(UUID userId, UUID groupId) {
        groups.join(userId, groupId);
    }

    @Override
    public void leave(UUID userId, UUID groupId) {
        groups.leave(userId, groupId);
    }
}
