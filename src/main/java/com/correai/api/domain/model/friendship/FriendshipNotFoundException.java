package com.correai.api.domain.model.friendship;

public class FriendshipNotFoundException extends RuntimeException {
    public FriendshipNotFoundException() {
        super("Friendship or user not found");
    }
}
