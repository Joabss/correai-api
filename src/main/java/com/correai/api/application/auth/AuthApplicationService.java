package com.correai.api.application.auth;

import com.correai.api.application.user.PasswordPolicy;
import com.correai.api.domain.model.auth.AuthToken;
import com.correai.api.domain.model.auth.InvalidCredentialsException;
import com.correai.api.domain.model.auth.InvalidTokenException;
import com.correai.api.domain.model.auth.OneTimeTokenType;
import com.correai.api.domain.model.user.User;
import com.correai.api.domain.model.user.UserCredentials;
import com.correai.api.domain.port.in.auth.AuthenticateTokenUseCase;
import com.correai.api.domain.port.in.auth.IssueAnonymousTokenUseCase;
import com.correai.api.domain.port.in.auth.LoginUseCase;
import com.correai.api.domain.port.in.auth.PasswordResetUseCase;
import com.correai.api.domain.port.in.auth.RefreshTokenUseCase;
import com.correai.api.domain.port.out.auth.OneTimeTokenPort;
import com.correai.api.domain.port.out.auth.PasswordHasherPort;
import com.correai.api.domain.port.out.auth.PasswordResetNotifierPort;
import com.correai.api.domain.port.out.auth.TokenPort;
import com.correai.api.domain.port.out.user.UserRepositoryPort;

import java.util.UUID;

public class AuthApplicationService implements IssueAnonymousTokenUseCase, AuthenticateTokenUseCase,
        LoginUseCase, RefreshTokenUseCase, PasswordResetUseCase {

    private final UserRepositoryPort userRepository;
    private final TokenPort tokenPort;
    private final PasswordHasherPort passwordHasher;
    private final OneTimeTokenPort oneTimeTokens;
    private final PasswordResetNotifierPort resetNotifier;
    // Verified when the e-mail is unknown so that the response time does not reveal it.
    private final String timingProtectionHash;

    public AuthApplicationService(
            UserRepositoryPort userRepository, TokenPort tokenPort, PasswordHasherPort passwordHasher,
            OneTimeTokenPort oneTimeTokens, PasswordResetNotifierPort resetNotifier) {
        this.userRepository = userRepository;
        this.tokenPort = tokenPort;
        this.passwordHasher = passwordHasher;
        this.oneTimeTokens = oneTimeTokens;
        this.resetNotifier = resetNotifier;
        this.timingProtectionHash = passwordHasher.hash(UUID.randomUUID().toString());
    }

    @Override
    public AuthToken issue() {
        User user = userRepository.save(User.anonymous());
        return newSession(user.id());
    }

    @Override
    public AuthToken login(String email, String password) {
        String candidate = password == null ? "" : password;
        UserCredentials credentials = userRepository.findCredentialsByEmail(User.normalizeEmail(email))
                .orElse(null);
        if (credentials == null) {
            passwordHasher.matches(candidate, timingProtectionHash);
            throw new InvalidCredentialsException();
        }
        if (!passwordHasher.matches(candidate, credentials.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        return newSession(credentials.user().id());
    }

    @Override
    public AuthToken refresh(String refreshToken) {
        UUID userId = oneTimeTokens.consume(refreshToken, OneTimeTokenType.REFRESH)
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired refresh token"));
        if (!userRepository.existsById(userId)) {
            throw new InvalidTokenException("Unknown user");
        }
        return newSession(userId);
    }

    @Override
    public void request(String email) {
        userRepository.findCredentialsByEmail(User.normalizeEmail(email)).ifPresent(credentials -> {
            String token = oneTimeTokens.issue(credentials.user().id(), OneTimeTokenType.PASSWORD_RESET);
            resetNotifier.send(credentials.user().email(), token);
        });
    }

    @Override
    public void reset(String resetToken, String newPassword) {
        PasswordPolicy.validate(newPassword);
        UUID userId = oneTimeTokens.consume(resetToken, OneTimeTokenType.PASSWORD_RESET)
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired reset token"));
        userRepository.updatePasswordHash(userId, passwordHasher.hash(newPassword));
        oneTimeTokens.revokeAll(userId, OneTimeTokenType.REFRESH);
    }

    @Override
    public UUID authenticate(String token) {
        UUID userId = tokenPort.verify(token);
        if (!userRepository.existsById(userId)) {
            throw new InvalidTokenException("Unknown user");
        }
        return userId;
    }

    private AuthToken newSession(UUID userId) {
        return tokenPort.issue(userId)
                .withRefreshToken(oneTimeTokens.issue(userId, OneTimeTokenType.REFRESH));
    }
}
