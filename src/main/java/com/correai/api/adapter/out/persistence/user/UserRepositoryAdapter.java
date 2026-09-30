package com.correai.api.adapter.out.persistence.user;

import com.correai.api.domain.model.user.User;
import com.correai.api.domain.port.out.user.UserRepositoryPort;
import org.springframework.stereotype.Component;

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
        UserEntity saved = jpaRepository.save(new UserEntity());
        return mapper.toDomain(saved);
    }

    @Override
    public boolean existsById(UUID userId) {
        return jpaRepository.existsById(userId);
    }
}

