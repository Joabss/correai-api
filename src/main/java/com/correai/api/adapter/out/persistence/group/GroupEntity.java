package com.correai.api.adapter.out.persistence.group;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "community_groups")
class GroupEntity {
    @Id
    UUID id;
    @Column(nullable = false, length = 100)
    String name;
    @Column(nullable = false)
    UUID createdBy;
    @Column(nullable = false)
    Instant createdAt;

    protected GroupEntity() {}

    GroupEntity(UUID id, String name, UUID createdBy, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }
}
