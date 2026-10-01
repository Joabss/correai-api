package com.correai.api.domain.model.activity;

public class ActivityNotFoundException extends RuntimeException {

    public ActivityNotFoundException() {
        super("Activity not found");
    }
}
