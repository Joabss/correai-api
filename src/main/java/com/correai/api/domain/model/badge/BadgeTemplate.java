package com.correai.api.domain.model.badge;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public record BadgeTemplate(UUID id, String name, String description, BadgeIcon icon, String color,
                            UUID createdBy, Instant createdAt) {
    private static final Pattern COLOR = Pattern.compile("#[0-9A-Fa-f]{6}");

    public BadgeTemplate {
        Objects.requireNonNull(id);
        Objects.requireNonNull(icon);
        Objects.requireNonNull(createdBy);
        Objects.requireNonNull(createdAt);
        if (name == null || name.isBlank() || name.trim().length() > 80) {
            throw new IllegalArgumentException("Badge name must contain between 1 and 80 characters");
        }
        if (description == null || description.isBlank() || description.trim().length() > 300) {
            throw new IllegalArgumentException("Badge description must contain between 1 and 300 characters");
        }
        if (color == null || !COLOR.matcher(color).matches()) {
            throw new IllegalArgumentException("Badge color must be a hexadecimal RGB value");
        }
        name = name.trim();
        description = description.trim();
        color = color.toUpperCase(java.util.Locale.ROOT);
    }
}
