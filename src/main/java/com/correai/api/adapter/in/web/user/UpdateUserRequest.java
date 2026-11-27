package com.correai.api.adapter.in.web.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank @Size(max = 100) String name,
        @Email @Size(max = 254) String email,
        @Size(max = 72) String currentPassword
) {
    @Override
    public String toString() {
        return "UpdateUserRequest[credentials redacted]";
    }
}
