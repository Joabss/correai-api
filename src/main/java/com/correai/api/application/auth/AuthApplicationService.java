package com.correai.api.application.auth;

import com.correai.api.domain.model.auth.AuthToken;
import com.correai.api.domain.model.auth.InvalidTokenException;
import com.correai.api.domain.model.user.User;
import com.correai.api.domain.port.in.auth.AuthenticateTokenUseCase;
import com.correai.api.domain.port.in.auth.IssueAnonymousTokenUseCase;
import com.correai.api.domain.port.out.auth.TokenPort;
import com.correai.api.domain.port.out.user.UserRepositoryPort;

import java.util.UUID;

public class AuthApplicationService implements IssueAnonymousTokenUseCase, AuthenticateTokenUseCase {

    private final UserRepositoryPort userRepository;
    private final TokenPort tokenPort;

    public AuthApplicationService(UserRepositoryPort userRepository, TokenPort tokenPort) {
        this.userRepository = userRepository;
        this.tokenPort = tokenPort;
    }

    @Override
    public AuthToken issue() {
        User user = userRepository.save(User.anonymous());
        return tokenPort.issue(user.id());
    }

    @Override
    public UUID authenticate(String token) {
        UUID userId = tokenPort.verify(token);
        if (!userRepository.existsById(userId)) {
            throw new InvalidTokenException("Unknown user");
        }
        return userId;
    }
}
