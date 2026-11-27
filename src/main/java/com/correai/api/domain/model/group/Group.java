package com.correai.api.domain.model.group;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Group(UUID id, String name, UUID createdBy, Instant createdAt, boolean member) {
    public Group {
        Objects.requireNonNull(id);
        Objects.requireNonNull(createdBy);
        Objects.requireNonNull(createdAt);
        if (name == null || name.isBlank() || name.length() > 100) {
            throw new IllegalArgumentException("Group name must contain between 1 and 100 characters");
        }
        name = name.trim();
    }
}
