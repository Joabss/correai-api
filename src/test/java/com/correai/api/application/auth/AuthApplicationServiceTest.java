package com.correai.api.application.auth;

import com.correai.api.domain.model.auth.AuthToken;
import com.correai.api.domain.model.auth.InvalidTokenException;
import com.correai.api.domain.model.user.User;
import com.correai.api.domain.port.out.auth.TokenPort;
import com.correai.api.domain.port.out.user.UserRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
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

    @InjectMocks
    private AuthApplicationService service;

    @Test
    void issue_shouldCreateAnonymousUserAndIssueToken() {
        UUID userId = UUID.randomUUID();
        AuthToken token = new AuthToken("jwt", userId, Instant.now().plusSeconds(60));
        when(userRepository.save(any(User.class))).thenReturn(User.reconstruct(userId, Instant.now()));
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
