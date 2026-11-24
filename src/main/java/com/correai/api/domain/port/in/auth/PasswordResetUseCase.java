package com.correai.api.domain.port.in.auth;

public interface PasswordResetUseCase {

    /** Sends a reset token if the e-mail belongs to an account; never reveals whether it does. */
    void request(String email);

    void reset(String resetToken, String newPassword);
}
