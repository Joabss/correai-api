package com.correai.api.domain.port.out.auth;

/**
 * Outbound port for one-way password hashing.
 */
public interface PasswordHasherPort {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String passwordHash);
}
