package com.correai.api.adapter.out.persistence.user;

import com.correai.api.domain.model.user.EmailAlreadyRegisteredException;
import com.correai.api.domain.model.user.User;
import com.correai.api.domain.model.user.UserCredentials;
import com.correai.api.domain.model.user.UserNotFoundException;
import com.correai.api.domain.port.out.user.UserRepositoryPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final SpringDataUserRepository jpaRepository;
    private final UserMapper mapper;

    public UserRepositoryAdapter(SpringDataUserRepository jpaRepository, UserMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public User save(User user) {
        UserEntity saved = jpaRepository.save(new UserEntity(null, user.name(), user.email(), null, null));
        return mapper.toDomain(saved);
    }

    @Override
    public User saveWithPassword(User user, String passwordHash) {
        try {
            UserEntity saved = jpaRepository.saveAndFlush(
                    new UserEntity(null, user.name(), user.email(), passwordHash, null));
            return mapper.toDomain(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyRegisteredException();
        }
    }

    @Override
    public User registerCredentials(UUID userId, String name, String email, String passwordHash) {
        UserEntity current = jpaRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        try {
            UserEntity saved = jpaRepository.saveAndFlush(
                    new UserEntity(userId, name, email, passwordHash, current.getCreatedAt()));
            return mapper.toDomain(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyRegisteredException();
        }
    }

    @Override
    public void updatePasswordHash(UUID userId, String passwordHash) {
        UserEntity current = jpaRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        jpaRepository.save(new UserEntity(
                userId, current.getName(), current.getEmail(), passwordHash, current.getCreatedAt()));
    }

    @Override
    public Optional<User> findById(UUID userId) {
        return jpaRepository.findById(userId).map(mapper::toDomain);
    }

    @Override
    public Optional<UserCredentials> findCredentialsByEmail(String email) {
        return jpaRepository.findByEmail(email)
                .filter(entity -> entity.getPasswordHash() != null)
                .map(entity -> new UserCredentials(mapper.toDomain(entity), entity.getPasswordHash()));
    }

    @Override
    public boolean existsById(UUID userId) {
        return jpaRepository.existsById(userId);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }
}

