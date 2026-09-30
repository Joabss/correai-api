package com.correai.api.domain.model.user;

import java.util.UUID;

public class UnknownUserException extends RuntimeException {

    public UnknownUserException(UUID userId) {
        super("Unknown user: " + userId);
    }
}