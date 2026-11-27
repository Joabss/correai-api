package com.correai.api.adapter.out.persistence.friendship;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "user_blocks", uniqueConstraints = @UniqueConstraint(
        name = "uk_user_block", columnNames = {"blocker_id", "blocked_id"}))
class UserBlockEntity {
    @Id UUID id;
    @Column(nullable = false) UUID blockerId;
    @Column(nullable = false) UUID blockedId;

    protected UserBlockEntity() {}

    UserBlockEntity(UUID blockerId, UUID blockedId) {
        this.id = UUID.randomUUID();
        this.blockerId = blockerId;
        this.blockedId = blockedId;
    }
}
