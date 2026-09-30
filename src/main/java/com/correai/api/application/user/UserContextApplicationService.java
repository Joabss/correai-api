package com.correai.api.application.user;

import com.correai.api.domain.model.user.User;
import com.correai.api.domain.model.user.UnknownUserException;
import com.correai.api.domain.port.in.user.EnsureUserUseCase;
import com.correai.api.domain.port.out.user.UserRepositoryPort;

import java.util.UUID;

public class UserContextApplicationService implements EnsureUserUseCase {

    private final UserRepositoryPort repository;

    public UserContextApplicationService(UserRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public UUID resolveOrCreate(UUID userIdOrNull) {
        if (userIdOrNull != null) {
            if (!repository.existsById(userIdOrNull)) {
                throw new UnknownUserException(userIdOrNull);
            }
            return userIdOrNull;
        }
        User saved = repository.save(User.anonymous());
        return saved.id();
    }
}

