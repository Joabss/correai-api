package com.correai.api.domain.port.out.auth;

/**
 * Outbound port that delivers a password reset token to the account owner.
 */
public interface PasswordResetNotifierPort {

    void send(String email, String resetToken);
}
