package com.correai.api.adapter.in.web.config;

import com.correai.api.domain.port.in.user.EnsureUserUseCase;
import com.correai.api.domain.model.user.UnknownUserException;
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
    private EnsureUserUseCase ensureUserUseCase;

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
        when(ensureUserUseCase.resolveOrCreate(userId)).thenReturn(userId);

        boolean result = interceptor.preHandle(request, response, handler);

        assertTrue(result);
        verify(request).setAttribute("userId", userId);
        verify(response, never()).setHeader(anyString(), anyString());
    }

    @Test
    void preHandle_withUnknownUserIdHeader_shouldRejectRequest() {
        when(request.getAttribute("userId")).thenReturn(null);
        when(request.getHeader("X-User-Id")).thenReturn(userId.toString());
        when(ensureUserUseCase.resolveOrCreate(userId)).thenThrow(new UnknownUserException(userId));

        boolean result = interceptor.preHandle(request, response, handler);

        assertFalse(result);
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(request, never()).setAttribute("userId", userId);
    }

    @Test
    void preHandle_withoutUserIdHeader_shouldCreateAnonymousUser() {
        UUID generatedId = UUID.randomUUID();
        when(request.getAttribute("userId")).thenReturn(null);
        when(request.getHeader("X-User-Id")).thenReturn(null);
        when(ensureUserUseCase.resolveOrCreate(null)).thenReturn(generatedId);

        boolean result = interceptor.preHandle(request, response, handler);

        assertTrue(result);
        verify(response).setHeader("X-User-Id", generatedId.toString());
        verify(request).setAttribute("userId", generatedId);
    }

    @Test
    void preHandle_withBlankUserIdHeader_shouldCreateAnonymousUser() {
        UUID generatedId = UUID.randomUUID();
        when(request.getAttribute("userId")).thenReturn(null);
        when(request.getHeader("X-User-Id")).thenReturn("   ");
        when(ensureUserUseCase.resolveOrCreate(null)).thenReturn(generatedId);

        boolean result = interceptor.preHandle(request, response, handler);

        assertTrue(result);
        verify(response).setHeader("X-User-Id", generatedId.toString());
        verify(request).setAttribute("userId", generatedId);
    }

    @Test
    void preHandle_withExistingAttribute_shouldReturnTrue() {
        when(request.getAttribute("userId")).thenReturn(userId);

        boolean result = interceptor.preHandle(request, response, handler);

        assertTrue(result);
        verify(request, never()).getHeader(anyString());
        verify(ensureUserUseCase, never()).resolveOrCreate(any());
    }
}

