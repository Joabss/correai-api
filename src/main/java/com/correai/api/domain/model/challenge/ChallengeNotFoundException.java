package com.correai.api.domain.model.challenge;

public class ChallengeNotFoundException extends RuntimeException {
    public ChallengeNotFoundException() {
        super("Challenge not found");
    }
}
