package com.correai.api.adapter.out.persistence.group;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

@Entity
@Table(name = "community_group_members", uniqueConstraints = @UniqueConstraint(
        name = "uk_community_group_member", columnNames = {"group_id", "user_id"}))
class GroupMemberEntity {
    @Id
    UUID id;
    @Column(nullable = false)
    UUID groupId;
    @Column(nullable = false)
    UUID userId;

    protected GroupMemberEntity() {}

    GroupMemberEntity(UUID groupId, UUID userId) {
        this.id = UUID.randomUUID();
        this.groupId = groupId;
        this.userId = userId;
    }
}
