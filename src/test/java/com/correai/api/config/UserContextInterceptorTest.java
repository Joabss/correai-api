package com.correai.api.config;

import com.correai.api.domain.user.User;
import com.correai.api.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
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
class UserContextInterceptorTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Object handler;

    @InjectMocks
    private UserContextInterceptor interceptor;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    void preHandle_withExistingUserIdHeader_shouldSetAttribute() {
        when(request.getAttribute("userId")).thenReturn(null);
        when(request.getHeader("X-User-Id")).thenReturn(userId.toString());

        boolean result = interceptor.preHandle(request, response, handler);

        assertTrue(result);
        verify(request).setAttribute("userId", userId);
    }

    @Test
    void preHandle_withoutUserIdHeader_shouldCreateAnonymousUser() {
        User anonymousUser = User.createAnonymous();
        try {
            java.lang.reflect.Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(anonymousUser, UUID.randomUUID());
        } catch (Exception e) {
            // Ignore for test
        }
        when(request.getAttribute("userId")).thenReturn(null);
        when(request.getHeader("X-User-Id")).thenReturn(null);
        when(userRepository.save(any(User.class))).thenReturn(anonymousUser);

        boolean result = interceptor.preHandle(request, response, handler);

        assertTrue(result);
        verify(userRepository).save(any(User.class));
        verify(response).setHeader("X-User-Id", anonymousUser.getId().toString());
        verify(request).setAttribute("userId", anonymousUser.getId());
    }

    @Test
    void preHandle_withBlankUserIdHeader_shouldCreateAnonymousUser() {
        User anonymousUser = User.createAnonymous();
        try {
            java.lang.reflect.Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(anonymousUser, UUID.randomUUID());
        } catch (Exception e) {
            // Ignore for test
        }
        when(request.getAttribute("userId")).thenReturn(null);
        when(request.getHeader("X-User-Id")).thenReturn("   ");
        when(userRepository.save(any(User.class))).thenReturn(anonymousUser);

        boolean result = interceptor.preHandle(request, response, handler);

        assertTrue(result);
        verify(userRepository).save(any(User.class));
        verify(response).setHeader("X-User-Id", anonymousUser.getId().toString());
        verify(request).setAttribute("userId", anonymousUser.getId());
    }

    @Test
    void preHandle_withExistingAttribute_shouldReturnTrue() {
        when(request.getAttribute("userId")).thenReturn(userId);

        boolean result = interceptor.preHandle(request, response, handler);

        assertTrue(result);
        verify(request, never()).getHeader(anyString());
        verify(userRepository, never()).save(any(User.class));
    }
}
