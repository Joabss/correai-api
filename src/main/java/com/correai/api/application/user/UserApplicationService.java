package com.correai.api.application.user;

import com.correai.api.domain.model.auth.AuthToken;
import com.correai.api.domain.model.auth.OneTimeTokenType;
import com.correai.api.domain.model.auth.InvalidCredentialsException;
import com.correai.api.domain.model.user.AccountAlreadyRegisteredException;
import com.correai.api.domain.model.user.EmailAlreadyRegisteredException;
import com.correai.api.domain.model.user.User;
import com.correai.api.domain.model.user.UserNotFoundException;
import com.correai.api.domain.port.in.user.ConvertToAccountUseCase;
import com.correai.api.domain.port.in.user.CreateUserUseCase;
import com.correai.api.domain.port.in.user.GetUserUseCase;
import com.correai.api.domain.port.in.user.UpdateUserUseCase;
import com.correai.api.domain.port.out.auth.OneTimeTokenPort;
import com.correai.api.domain.port.out.auth.PasswordHasherPort;
import com.correai.api.domain.port.out.auth.TokenPort;
import com.correai.api.domain.port.out.user.UserRepositoryPort;

import java.util.UUID;

public class UserApplicationService implements CreateUserUseCase, GetUserUseCase, ConvertToAccountUseCase,
        UpdateUserUseCase {

    private final UserRepositoryPort userRepository;
    private final TokenPort tokenPort;
    private final PasswordHasherPort passwordHasher;
    private final OneTimeTokenPort oneTimeTokens;

    public UserApplicationService(
            UserRepositoryPort userRepository, TokenPort tokenPort, PasswordHasherPort passwordHasher,
            OneTimeTokenPort oneTimeTokens) {
        this.userRepository = userRepository;
        this.tokenPort = tokenPort;
        this.passwordHasher = passwordHasher;
        this.oneTimeTokens = oneTimeTokens;
    }

    @Override
    public AuthToken create(String name, String email, String password) {
        User newUser = User.registered(name, email);
        PasswordPolicy.validate(password);
        if (userRepository.existsByEmail(newUser.email())) {
            throw new EmailAlreadyRegisteredException();
        }
        User saved = userRepository.saveWithPassword(newUser, passwordHasher.hash(password));
        return tokenPort.issue(saved.id())
                .withRefreshToken(oneTimeTokens.issue(saved.id(), OneTimeTokenType.REFRESH));
    }

    @Override
    public User convertToAccount(UUID userId, String name, String email, String password) {
        User current = getById(userId);
        if (!current.isAnonymous()) {
            throw new AccountAlreadyRegisteredException();
        }
        User registered = User.registered(name, email);
        PasswordPolicy.validate(password);
        if (userRepository.existsByEmail(registered.email())) {
            throw new EmailAlreadyRegisteredException();
        }
        return userRepository.registerCredentials(
                userId, registered.name(), registered.email(), passwordHasher.hash(password));
    }

    @Override
    public User getById(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Override
    public User update(UUID userId, String name, String email, String currentPassword) {
        User current = getById(userId);
        if (name == null || name.isBlank() || name.trim().length() > 100) {
            throw new IllegalArgumentException("Invalid name");
        }
        if (current.isAnonymous()) {
            if (email != null) {
                throw new IllegalArgumentException("Use account conversion to set an email");
            }
            return userRepository.updateProfile(userId, name.trim(), null, null);
        }
        User updated = User.registered(name, email);
        if (!updated.email().equals(current.email())) {
            var credentials = userRepository.findCredentialsByEmail(current.email())
                    .filter(found -> found.user().id().equals(userId))
                    .orElseThrow(InvalidCredentialsException::new);
            if (currentPassword == null || !passwordHasher.matches(currentPassword, credentials.passwordHash())) {
                throw new InvalidCredentialsException();
            }
            if (userRepository.existsByEmail(updated.email())) {
                throw new EmailAlreadyRegisteredException();
            }
        }
        return userRepository.updateProfile(userId, updated.name(), updated.email(), current.email());
    }
}
