package com.correai.api.domain.port.out.user;

import com.correai.api.domain.model.user.User;
import java.util.UUID;

/**
 * Outbound port for User persistence, implemented by an infrastructure adapter.
 */
public interface UserRepositoryPort {

    User save(User user);

    boolean existsById(UUID userId);
}

