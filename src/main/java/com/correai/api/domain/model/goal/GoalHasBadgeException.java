package com.correai.api.domain.model.goal;

public class GoalHasBadgeException extends RuntimeException {
    public GoalHasBadgeException() {
        super("A challenge with an earned badge cannot be deleted");
    }
}
