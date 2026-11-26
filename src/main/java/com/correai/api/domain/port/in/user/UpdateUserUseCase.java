package com.correai.api.domain.port.in.user;

import com.correai.api.domain.model.user.User;
import java.util.UUID;

public interface UpdateUserUseCase {
    User update(UUID userId, String name, String email, String currentPassword);
}
