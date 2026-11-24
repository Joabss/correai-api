package com.correai.api.domain.model.user;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * Pure domain model for a User. No framework/persistence concerns here.
 * Anonymous users have no name and no e-mail.
 */
public record User(UUID id, String name, String email, Instant createdAt) {

    /** Creates a brand-new anonymous user (not yet persisted). */
    public static User anonymous() {
        return new User(null, null, null, null);
    }

    /** Creates a brand-new registered user (not yet persisted). */
    public static User registered(String name, String email) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name must not be blank");
        }
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.indexOf('@') < 1) {
            throw new IllegalArgumentException("Email is invalid");
        }
        return new User(null, name.trim(), normalizedEmail, null);
    }

    /** Reconstructs a User from persistence. */
    public static User reconstruct(UUID id, String name, String email, Instant createdAt) {
        return new User(id, name, email, createdAt);
    }

    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    public boolean isAnonymous() {
        return email == null;
    }

}
