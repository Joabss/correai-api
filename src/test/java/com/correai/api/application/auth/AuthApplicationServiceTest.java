package com.correai.api.application.auth;

import com.correai.api.domain.model.auth.AuthToken;
import com.correai.api.domain.model.auth.InvalidCredentialsException;
import com.correai.api.domain.model.auth.InvalidTokenException;
import com.correai.api.domain.model.auth.OneTimeTokenType;
import com.correai.api.domain.model.user.User;
import com.correai.api.domain.model.user.UserCredentials;
import com.correai.api.domain.port.out.auth.OneTimeTokenPort;
import com.correai.api.domain.port.out.auth.PasswordHasherPort;
import com.correai.api.domain.port.out.auth.PasswordResetNotifierPort;
import com.correai.api.domain.port.out.auth.TokenPort;
import com.correai.api.domain.port.out.user.UserRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthApplicationServiceTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private TokenPort tokenPort;

    @Mock
    private PasswordHasherPort passwordHasher;

    @Mock
    private OneTimeTokenPort oneTimeTokens;

    @Mock
    private PasswordResetNotifierPort resetNotifier;

    @InjectMocks
    private AuthApplicationService service;

    @Test
    void login_withValidCredentials_shouldIssueTokenWithRefreshToken() {
        UUID userId = UUID.randomUUID();
        AuthToken token = new AuthToken("jwt", userId, Instant.now().plusSeconds(60));
        User user = User.reconstruct(userId, "Ana", "ana@example.com", Instant.now());
        when(userRepository.findCredentialsByEmail("ana@example.com"))
                .thenReturn(Optional.of(new UserCredentials(user, "hashed")));
        when(passwordHasher.matches("secret-123", "hashed")).thenReturn(true);
        when(tokenPort.issue(userId)).thenReturn(token);
        when(oneTimeTokens.issue(userId, OneTimeTokenType.REFRESH)).thenReturn("refresh-1");

        assertEquals(token.withRefreshToken("refresh-1"), service.login(" Ana@Example.com ", "secret-123"));
    }

    @Test
    void login_withWrongPassword_shouldThrowInvalidCredentials() {
        User user = User.reconstruct(UUID.randomUUID(), "Ana", "ana@example.com", Instant.now());
        when(userRepository.findCredentialsByEmail("ana@example.com"))
                .thenReturn(Optional.of(new UserCredentials(user, "hashed")));
        when(passwordHasher.matches("wrong", "hashed")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> service.login("ana@example.com", "wrong"));
        verifyNoInteractions(tokenPort, oneTimeTokens);
    }

    @Test
    void login_withUnknownEmail_shouldStillVerifyAHashAndThrow() {
        when(userRepository.findCredentialsByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> service.login("nobody@example.com", "secret-123"));
        verify(passwordHasher).matches(eq("secret-123"), any());
        verifyNoInteractions(tokenPort, oneTimeTokens);
    }

    @Test
    void refresh_withValidToken_shouldRotateTokens() {
        UUID userId = UUID.randomUUID();
        AuthToken token = new AuthToken("jwt", userId, Instant.now().plusSeconds(60));
        when(oneTimeTokens.consume("old", OneTimeTokenType.REFRESH)).thenReturn(Optional.of(userId));
        when(userRepository.existsById(userId)).thenReturn(true);
        when(tokenPort.issue(userId)).thenReturn(token);
        when(oneTimeTokens.issue(userId, OneTimeTokenType.REFRESH)).thenReturn("new");

        assertEquals(token.withRefreshToken("new"), service.refresh("old"));
    }

    @Test
    void refresh_withInvalidToken_shouldThrowInvalidToken() {
        when(oneTimeTokens.consume("bad", OneTimeTokenType.REFRESH)).thenReturn(Optional.empty());

        assertThrows(InvalidTokenException.class, () -> service.refresh("bad"));
        verifyNoInteractions(tokenPort);
    }

    @Test
    void refresh_withDeletedUser_shouldThrowInvalidToken() {
        UUID userId = UUID.randomUUID();
        when(oneTimeTokens.consume("old", OneTimeTokenType.REFRESH)).thenReturn(Optional.of(userId));
        when(userRepository.existsById(userId)).thenReturn(false);

        assertThrows(InvalidTokenException.class, () -> service.refresh("old"));
    }

    @Test
    void request_withKnownEmail_shouldIssueAndSendResetToken() {
        UUID userId = UUID.randomUUID();
        User user = User.reconstruct(userId, "Ana", "ana@example.com", Instant.now());
        when(userRepository.findCredentialsByEmail("ana@example.com"))
                .thenReturn(Optional.of(new UserCredentials(user, "hashed")));
        when(oneTimeTokens.issue(userId, OneTimeTokenType.PASSWORD_RESET)).thenReturn("reset-1");

        service.request("Ana@example.com");

        verify(resetNotifier).send("ana@example.com", "reset-1");
    }

    @Test
    void request_withUnknownEmail_shouldDoNothing() {
        when(userRepository.findCredentialsByEmail("nobody@example.com")).thenReturn(Optional.empty());

        service.request("nobody@example.com");

        verifyNoInteractions(oneTimeTokens, resetNotifier);
    }

    @Test
    void reset_withValidToken_shouldUpdatePasswordAndRevokeRefreshTokens() {
        UUID userId = UUID.randomUUID();
        when(oneTimeTokens.consume("reset-1", OneTimeTokenType.PASSWORD_RESET)).thenReturn(Optional.of(userId));
        when(passwordHasher.hash("new-secret-1")).thenReturn("new-hash");

        service.reset("reset-1", "new-secret-1");

        verify(userRepository).updatePasswordHash(userId, "new-hash");
        verify(oneTimeTokens).revokeAll(userId, OneTimeTokenType.REFRESH);
    }

    @Test
    void reset_withInvalidToken_shouldThrowAndNotUpdate() {
        when(oneTimeTokens.consume("bad", OneTimeTokenType.PASSWORD_RESET)).thenReturn(Optional.empty());

        assertThrows(InvalidTokenException.class, () -> service.reset("bad", "new-secret-1"));
        verify(userRepository, never()).updatePasswordHash(any(), any());
    }

    @Test
    void reset_withWeakPassword_shouldThrowBeforeConsumingToken() {
        assertThrows(IllegalArgumentException.class, () -> service.reset("reset-1", "short"));
        verifyNoInteractions(oneTimeTokens);
    }

    @Test
    void issue_shouldCreateAnonymousUserAndIssueToken() {
        UUID userId = UUID.randomUUID();
        AuthToken token = new AuthToken("jwt", userId, Instant.now().plusSeconds(60));
        when(userRepository.save(any(User.class))).thenReturn(User.reconstruct(userId, null, null, Instant.now()));
        when(tokenPort.issue(userId)).thenReturn(token);

        assertEquals(token, service.issue());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void authenticate_withValidTokenAndExistingUser_shouldReturnUserId() {
        UUID userId = UUID.randomUUID();
        when(tokenPort.verify("jwt")).thenReturn(userId);
        when(userRepository.existsById(userId)).thenReturn(true);

        assertEquals(userId, service.authenticate("jwt"));
    }

    @Test
    void authenticate_withUnknownUser_shouldThrowInvalidToken() {
        UUID userId = UUID.randomUUID();
        when(tokenPort.verify("jwt")).thenReturn(userId);
        when(userRepository.existsById(userId)).thenReturn(false);

        assertThrows(InvalidTokenException.class, () -> service.authenticate("jwt"));
    }

    @Test
    void authenticate_withInvalidToken_shouldPropagateException() {
        when(tokenPort.verify("bad")).thenThrow(new InvalidTokenException("Malformed token"));

        assertThrows(InvalidTokenException.class, () -> service.authenticate("bad"));
        verifyNoInteractions(userRepository);
    }
}
