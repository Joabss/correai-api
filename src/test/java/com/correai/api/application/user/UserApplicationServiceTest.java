package com.correai.api.application.user;

import com.correai.api.domain.model.auth.AuthToken;
import com.correai.api.domain.model.auth.OneTimeTokenType;
import com.correai.api.domain.model.user.AccountAlreadyRegisteredException;
import com.correai.api.domain.model.user.EmailAlreadyRegisteredException;
import com.correai.api.domain.model.user.User;
import com.correai.api.domain.model.user.UserNotFoundException;
import com.correai.api.domain.port.out.auth.OneTimeTokenPort;
import com.correai.api.domain.port.out.auth.PasswordHasherPort;
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
class UserApplicationServiceTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private TokenPort tokenPort;

    @Mock
    private PasswordHasherPort passwordHasher;

    @Mock
    private OneTimeTokenPort oneTimeTokens;

    @InjectMocks
    private UserApplicationService service;

    @Test
    void convertToAccount_shouldRegisterCredentialsForAnonymousUser() {
        UUID userId = UUID.randomUUID();
        User converted = User.reconstruct(userId, "Ana", "ana@example.com", Instant.now());
        when(userRepository.findById(userId))
                .thenReturn(Optional.of(User.reconstruct(userId, null, null, Instant.now())));
        when(userRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(passwordHasher.hash("secret-123")).thenReturn("hashed");
        when(userRepository.registerCredentials(userId, "Ana", "ana@example.com", "hashed")).thenReturn(converted);

        assertEquals(converted, service.convertToAccount(userId, " Ana ", "Ana@Example.com", "secret-123"));
    }

    @Test
    void convertToAccount_withExistingAccount_shouldThrowConflict() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId))
                .thenReturn(Optional.of(User.reconstruct(userId, "Ana", "ana@example.com", Instant.now())));

        assertThrows(AccountAlreadyRegisteredException.class,
                () -> service.convertToAccount(userId, "Ana", "other@example.com", "secret-123"));
    }

    @Test
    void convertToAccount_withTakenEmail_shouldThrowConflict() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId))
                .thenReturn(Optional.of(User.reconstruct(userId, null, null, Instant.now())));
        when(userRepository.existsByEmail("ana@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyRegisteredException.class,
                () -> service.convertToAccount(userId, "Ana", "ana@example.com", "secret-123"));
        verify(userRepository, never()).registerCredentials(any(), any(), any(), any());
    }

    @Test
    void create_shouldSaveUserWithHashedPasswordAndIssueToken() {
        UUID userId = UUID.randomUUID();
        AuthToken token = new AuthToken("jwt", userId, Instant.now().plusSeconds(60));
        when(userRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(passwordHasher.hash("secret-123")).thenReturn("hashed");
        when(userRepository.saveWithPassword(any(User.class), eq("hashed")))
                .thenReturn(User.reconstruct(userId, "Ana", "ana@example.com", Instant.now()));
        when(tokenPort.issue(userId)).thenReturn(token);
        when(oneTimeTokens.issue(userId, OneTimeTokenType.REFRESH)).thenReturn("refresh-1");

        assertEquals(token.withRefreshToken("refresh-1"), service.create(" Ana ", "Ana@Example.com", "secret-123"));
        verify(userRepository).saveWithPassword(
                argThat(user -> "Ana".equals(user.name()) && "ana@example.com".equals(user.email())),
                eq("hashed"));
    }

    @Test
    void create_withExistingEmail_shouldThrowConflict() {
        when(userRepository.existsByEmail("ana@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyRegisteredException.class,
                () -> service.create("Ana", "ana@example.com", "secret-123"));
        verify(userRepository, never()).saveWithPassword(any(), any());
    }

    @Test
    void create_withInvalidInput_shouldThrowAndNotSave() {
        assertThrows(IllegalArgumentException.class, () -> service.create(" ", "ana@example.com", "secret-123"));
        assertThrows(IllegalArgumentException.class, () -> service.create("Ana", "invalid", "secret-123"));
        assertThrows(IllegalArgumentException.class, () -> service.create("Ana", "ana@example.com", "short"));
        assertThrows(IllegalArgumentException.class, () -> service.create("Ana", "ana@example.com", null));
        assertThrows(IllegalArgumentException.class,
                () -> service.create("Ana", "ana@example.com", "x".repeat(73)));
        verifyNoInteractions(userRepository, tokenPort, passwordHasher);
    }

    @Test
    void getById_shouldReturnUser() {
        UUID userId = UUID.randomUUID();
        User user = User.reconstruct(userId, "Ana", "ana@example.com", Instant.now());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertEquals(user, service.getById(userId));
    }

    @Test
    void getById_withUnknownUser_shouldThrow() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> service.getById(userId));
    }
}
