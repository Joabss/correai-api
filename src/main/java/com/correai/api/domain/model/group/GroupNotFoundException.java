package com.correai.api.domain.model.group;

public class GroupNotFoundException extends RuntimeException {
    public GroupNotFoundException() {
        super("Group not found");
    }
}
