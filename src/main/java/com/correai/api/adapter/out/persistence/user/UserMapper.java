package com.correai.api.adapter.out.persistence.user;

import com.correai.api.domain.model.user.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toDomain(UserEntity entity) {
        return User.reconstruct(entity.getId(), entity.getCreatedAt());
    }
}

