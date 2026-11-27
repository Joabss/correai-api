package com.correai.api.domain.model.admin;

public class AdminAccessDeniedException extends RuntimeException {
    public AdminAccessDeniedException() {
        super("Administrator access denied");
    }
}
