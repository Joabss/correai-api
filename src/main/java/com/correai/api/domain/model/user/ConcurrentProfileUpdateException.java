package com.correai.api.domain.model.user;

public class ConcurrentProfileUpdateException extends RuntimeException {
    public ConcurrentProfileUpdateException() {
        super("Profile changed concurrently; retry the request");
    }
}
