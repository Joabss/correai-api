package com.correai.api.adapter.out.persistence.group;

import com.correai.api.domain.model.group.Group;
import com.correai.api.domain.model.group.GroupNotFoundException;
import com.correai.api.domain.model.pagination.PageQuery;
import com.correai.api.domain.model.pagination.PageResult;
import com.correai.api.domain.port.out.group.GroupRepositoryPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import java.util.UUID;

@Component
public class GroupRepositoryAdapter implements GroupRepositoryPort {
    private final SpringDataGroupRepository groups;
    private final SpringDataGroupMemberRepository members;

    public GroupRepositoryAdapter(SpringDataGroupRepository groups, SpringDataGroupMemberRepository members) {
        this.groups = groups;
        this.members = members;
    }

    @Override
    @Transactional
    public Group create(Group group) {
        groups.save(new GroupEntity(group.id(), group.name(), group.createdBy(), group.createdAt()));
        members.save(new GroupMemberEntity(group.id(), group.createdBy()));
        return group;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Group> search(UUID userId, String name, PageQuery page) {
        var result = groups.findByNameContainingIgnoreCaseOrderByNameAscIdAsc(
                name, PageRequest.of(page.page(), page.size()));
        return new PageResult<>(result.getContent().stream().map(g -> new Group(
                g.id, g.name, g.createdBy, g.createdAt,
                members.existsByGroupIdAndUserId(g.id, userId))).toList(),
                page.page(), page.size(), result.getTotalElements());
    }

    @Override
    @Transactional
    public void join(UUID userId, UUID groupId) {
        groups.lockById(groupId).orElseThrow(GroupNotFoundException::new);
        if (!members.existsByGroupIdAndUserId(groupId, userId)) {
            members.save(new GroupMemberEntity(groupId, userId));
        }
    }

    @Override
    @Transactional
    public void leave(UUID userId, UUID groupId) {
        groups.lockById(groupId).orElseThrow(GroupNotFoundException::new);
        members.findByGroupIdAndUserId(groupId, userId).ifPresent(members::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isMember(UUID userId, UUID groupId) {
        return members.existsByGroupIdAndUserId(groupId, userId);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean isMemberLocked(UUID userId, UUID groupId) {
        groups.lockById(groupId).orElseThrow(GroupNotFoundException::new);
        return members.existsByGroupIdAndUserId(groupId, userId);
    }
}
