package com.correai.api.domain.port.in.user;

import com.correai.api.domain.model.user.User;

import java.util.UUID;

public interface ConvertToAccountUseCase {

    /** Turns a user without credentials (anonymous or name-only) into a registered account. */
    User convertToAccount(UUID userId, String name, String email, String password);
}
