package com.correai.api.adapter.in.web.user;

import com.correai.api.domain.model.user.User;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, String name, String email, boolean anonymous, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.id(), user.name(), user.email(), user.isAnonymous(), user.createdAt());
    }
}
