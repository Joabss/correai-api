package com.correai.api.adapter.out.persistence.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "administrators")
class AdminEntity {
    @Id UUID userId;
    @Column(nullable = false) Instant grantedAt;
    @Column(nullable = false) UUID grantedBy;

    protected AdminEntity() {}

    AdminEntity(UUID userId, UUID grantedBy, Instant grantedAt) {
        this.userId = userId;
        this.grantedBy = grantedBy;
        this.grantedAt = grantedAt;
    }
}
