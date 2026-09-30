package com.correai.api.application.user;

import com.correai.api.domain.model.user.User;
import com.correai.api.domain.model.user.UnknownUserException;
import com.correai.api.domain.port.out.user.UserRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserContextApplicationServiceTest {

    @Mock
    private UserRepositoryPort repository;

    @InjectMocks
    private UserContextApplicationService service;

    @Test
    void resolveOrCreate_withExistingUserId_shouldReturnSameId() {
        UUID userId = UUID.randomUUID();
        when(repository.existsById(userId)).thenReturn(true);

        UUID result = service.resolveOrCreate(userId);

        assertEquals(userId, result);
        verify(repository, never()).save(any(User.class));
    }

    @Test
    void resolveOrCreate_withUnknownUserId_shouldRejectUser() {
        UUID userId = UUID.randomUUID();
        when(repository.existsById(userId)).thenReturn(false);

        assertThrows(UnknownUserException.class, () -> service.resolveOrCreate(userId));
        verify(repository, never()).save(any(User.class));
    }

    @Test
    void resolveOrCreate_withNull_shouldCreateAnonymousUser() {
        UUID generatedId = UUID.randomUUID();
        when(repository.save(any(User.class))).thenReturn(User.reconstruct(generatedId, null));

        UUID result = service.resolveOrCreate(null);

        assertEquals(generatedId, result);
        verify(repository).save(any(User.class));
    }
}

