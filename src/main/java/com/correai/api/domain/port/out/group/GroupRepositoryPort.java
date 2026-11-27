package com.correai.api.domain.port.out.group;

import com.correai.api.domain.model.group.Group;
import com.correai.api.domain.model.pagination.PageQuery;
import com.correai.api.domain.model.pagination.PageResult;
import java.util.UUID;

public interface GroupRepositoryPort {
    Group create(Group group);
    PageResult<Group> search(UUID userId, String name, PageQuery page);
    void join(UUID userId, UUID groupId);
    void leave(UUID userId, UUID groupId);
    boolean isMember(UUID userId, UUID groupId);
    boolean isMemberLocked(UUID userId, UUID groupId);
}
