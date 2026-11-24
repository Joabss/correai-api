package com.correai.api.domain.port.out.user;

import com.correai.api.domain.model.user.User;
import com.correai.api.domain.model.user.UserCredentials;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for User persistence, implemented by an infrastructure adapter.
 */
public interface UserRepositoryPort {

    User save(User user);

    /**
     * Saves a user together with its password hash.
     *
     * @throws com.correai.api.domain.model.user.EmailAlreadyRegisteredException if the e-mail is taken
     */
    User saveWithPassword(User user, String passwordHash);

    /**
     * Sets name, e-mail and password hash of an existing user.
     *
     * @throws com.correai.api.domain.model.user.EmailAlreadyRegisteredException if the e-mail is taken
     * @throws com.correai.api.domain.model.user.UserNotFoundException if the user does not exist
     */
    User registerCredentials(UUID userId, String name, String email, String passwordHash);

    void updatePasswordHash(UUID userId, String passwordHash);

    boolean existsById(UUID userId);

    boolean existsByEmail(String email);

    Optional<User> findById(UUID userId);

    Optional<UserCredentials> findCredentialsByEmail(String email);
}

