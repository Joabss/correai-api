package com.correai.api.adapter.in.web.config;

import com.correai.api.domain.model.auth.InvalidTokenException;
import com.correai.api.domain.port.in.auth.AuthenticateTokenUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
public class UserContextInterceptor implements HandlerInterceptor {

    public static final String USER_REQUEST_ATTR = "userId";

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthenticateTokenUseCase authenticateTokenUseCase;

    public UserContextInterceptor(AuthenticateTokenUseCase authenticateTokenUseCase) {
        this.authenticateTokenUseCase = authenticateTokenUseCase;
    }

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler
    ) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || request.getAttribute(USER_REQUEST_ATTR) != null) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new InvalidTokenException("Missing bearer token");
        }

        UUID userId = authenticateTokenUseCase.authenticate(authorization.substring(BEARER_PREFIX.length()).trim());
        request.setAttribute(USER_REQUEST_ATTR, userId);
        return true;
    }
}
