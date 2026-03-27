package com.correai.api.config;

import com.correai.api.domain.user.User;
import com.correai.api.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
public class UserContextInterceptor implements HandlerInterceptor {

    private static final String USER_HEADER = "X-User-Id";
    private static final String USER_REQUEST_ATTR = "userId";

    private final UserRepository userRepository;

    public UserContextInterceptor(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {

        if (request.getAttribute(USER_REQUEST_ATTR) != null) {
            return true;
        }

        String userIdHeader = request.getHeader(USER_HEADER);
        UUID userId;

        if (userIdHeader == null || userIdHeader.isBlank()) {
            User user = userRepository.save(User.createAnonymous());
            userId = user.getId();
            response.setHeader(USER_HEADER, userId.toString());
        } else {
            userId = UUID.fromString(userIdHeader);
        }

        request.setAttribute("userId", userId);
        return true;
    }
}
