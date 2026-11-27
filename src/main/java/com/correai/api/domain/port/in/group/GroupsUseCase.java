package com.correai.api.domain.port.in.group;

import com.correai.api.domain.model.group.Group;
import com.correai.api.domain.model.pagination.PageQuery;
import com.correai.api.domain.model.pagination.PageResult;
import java.util.UUID;

public interface GroupsUseCase {
    Group create(UUID userId, String name);
    PageResult<Group> search(UUID userId, String name, PageQuery page);
    void join(UUID userId, UUID groupId);
    void leave(UUID userId, UUID groupId);
}
