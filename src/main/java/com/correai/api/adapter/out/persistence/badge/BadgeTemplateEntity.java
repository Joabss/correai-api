package com.correai.api.adapter.out.persistence.badge;

import com.correai.api.domain.model.badge.BadgeIcon;
import com.correai.api.domain.model.badge.BadgeTemplate;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "badge_templates")
class BadgeTemplateEntity {
    @Id UUID id;
    @Column(nullable = false, length = 80) String name;
    @Column(nullable = false, length = 300) String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) BadgeIcon icon;
    @Column(nullable = false, length = 7) String color;
    @Column(nullable = false) UUID createdBy;
    @Column(nullable = false) Instant createdAt;

    protected BadgeTemplateEntity() {}

    BadgeTemplateEntity(BadgeTemplate template) {
        id = template.id();
        name = template.name();
        description = template.description();
        icon = template.icon();
        color = template.color();
        createdBy = template.createdBy();
        createdAt = template.createdAt();
    }

    BadgeTemplate domain() {
        return new BadgeTemplate(id, name, description, icon, color, createdBy, createdAt);
    }
}
