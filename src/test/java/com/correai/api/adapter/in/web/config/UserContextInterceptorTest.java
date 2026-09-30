package com.correai.api.adapter.in.web.config;

import com.correai.api.domain.model.auth.InvalidTokenException;
import com.correai.api.domain.port.in.auth.AuthenticateTokenUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserContextInterceptorTest {

    @Mock
    private AuthenticateTokenUseCase authenticateTokenUseCase;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private UserContextInterceptor interceptor;

    @Test
    void preHandle_withValidBearerToken_shouldSetUserAttribute() {
        UUID userId = UUID.randomUUID();
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer abc.def.ghi");
        when(authenticateTokenUseCase.authenticate("abc.def.ghi")).thenReturn(userId);

        assertTrue(interceptor.preHandle(request, response, new Object()));

        verify(request).setAttribute("userId", userId);
    }

    @Test
    void preHandle_withoutAuthorizationHeader_shouldThrowInvalidToken() {
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn(null);

        assertThrows(InvalidTokenException.class, () -> interceptor.preHandle(request, response, new Object()));
        verifyNoInteractions(authenticateTokenUseCase);
    }

    @Test
    void preHandle_withNonBearerScheme_shouldThrowInvalidToken() {
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Basic abc");

        assertThrows(InvalidTokenException.class, () -> interceptor.preHandle(request, response, new Object()));
    }

    @Test
    void preHandle_withInvalidToken_shouldPropagateException() {
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer bad");
        when(authenticateTokenUseCase.authenticate("bad")).thenThrow(new InvalidTokenException("Malformed token"));

        assertThrows(InvalidTokenException.class, () -> interceptor.preHandle(request, response, new Object()));
        verify(request, never()).setAttribute(eq("userId"), any());
    }

    @Test
    void preHandle_withPreflightRequest_shouldSkipAuthentication() {
        when(request.getMethod()).thenReturn("OPTIONS");

        assertTrue(interceptor.preHandle(request, response, new Object()));
        verifyNoInteractions(authenticateTokenUseCase);
    }

    @Test
    void preHandle_withExistingAttribute_shouldReturnTrue() {
        when(request.getMethod()).thenReturn("GET");
        when(request.getAttribute("userId")).thenReturn(UUID.randomUUID());

        assertTrue(interceptor.preHandle(request, response, new Object()));
        verify(request, never()).getHeader(anyString());
    }
}
