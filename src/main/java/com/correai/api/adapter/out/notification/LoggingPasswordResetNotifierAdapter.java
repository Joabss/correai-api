package com.correai.api.adapter.out.notification;

import com.correai.api.domain.port.out.auth.PasswordResetNotifierPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Placeholder notifier: no e-mail provider is configured yet. The token is only logged when
 * {@code app.security.password-reset.log-token=true} (development only).
 */
@Component
public class LoggingPasswordResetNotifierAdapter implements PasswordResetNotifierPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingPasswordResetNotifierAdapter.class);

    private final boolean logToken;

    public LoggingPasswordResetNotifierAdapter(
            @Value("${app.security.password-reset.log-token:false}") boolean logToken) {
        this.logToken = logToken;
    }

    @Override
    public void send(String email, String resetToken) {
        if (logToken) {
            log.warn("Password reset token for {}: {}", email, resetToken);
        } else {
            log.info("Password reset requested, but no e-mail provider is configured");
        }
    }
}
