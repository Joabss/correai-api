package com.correai.api.domain.model.user;

/**
 * A user together with the stored password hash, used only to verify logins.
 */
public record UserCredentials(User user, String passwordHash) {
}
