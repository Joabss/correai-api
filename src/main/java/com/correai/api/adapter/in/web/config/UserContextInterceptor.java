package com.correai.api.adapter.in.web.config;

import com.correai.api.domain.port.in.user.EnsureUserUseCase;
import com.correai.api.domain.model.user.UnknownUserException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
public class UserContextInterceptor implements HandlerInterceptor {

    private static final String USER_HEADER = "X-User-Id";
    private static final String USER_REQUEST_ATTR = "userId";

    private final EnsureUserUseCase ensureUserUseCase;

    public UserContextInterceptor(EnsureUserUseCase ensureUserUseCase) {
        this.ensureUserUseCase = ensureUserUseCase;
    }

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler
    ) {

        if (request.getAttribute(USER_REQUEST_ATTR) != null) {
            return true;
        }

        String userIdHeader = request.getHeader(USER_HEADER);
        UUID providedUserId;
        try {
            providedUserId = (userIdHeader == null || userIdHeader.isBlank())
                    ? null
                    : UUID.fromString(userIdHeader);
        } catch (IllegalArgumentException exception) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        UUID userId;
        try {
            userId = ensureUserUseCase.resolveOrCreate(providedUserId);
        } catch (UnknownUserException exception) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        if (providedUserId == null) {
            response.setHeader(USER_HEADER, userId.toString());
        }

        request.setAttribute(USER_REQUEST_ATTR, userId);
        return true;
    }
}

