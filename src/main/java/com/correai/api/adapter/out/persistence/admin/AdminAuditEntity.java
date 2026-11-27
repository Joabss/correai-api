package com.correai.api.adapter.out.persistence.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "admin_audit")
class AdminAuditEntity {
    @Id UUID id;
    @Column(nullable = false) UUID actorId;
    @Column(nullable = false) UUID targetId;
    @Column(nullable = false, length = 20) String action;
    @Column(nullable = false) Instant createdAt;

    protected AdminAuditEntity() {}

    AdminAuditEntity(UUID actorId, UUID targetId, String action, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.actorId = actorId;
        this.targetId = targetId;
        this.action = action;
        this.createdAt = createdAt;
    }
}
