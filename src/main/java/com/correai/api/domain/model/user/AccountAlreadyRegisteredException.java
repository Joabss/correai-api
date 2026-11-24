package com.correai.api.domain.model.user;

public class AccountAlreadyRegisteredException extends RuntimeException {

    public AccountAlreadyRegisteredException() {
        super("User already has an account");
    }
}
