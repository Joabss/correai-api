package com.correai.api.domain.model.friendship;

public class FriendshipConflictException extends RuntimeException {
    public FriendshipConflictException() {
        super("A request from this user is already pending");
    }
}
