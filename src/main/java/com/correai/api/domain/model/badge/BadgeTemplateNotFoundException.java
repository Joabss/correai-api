package com.correai.api.domain.model.badge;

public class BadgeTemplateNotFoundException extends RuntimeException {
    public BadgeTemplateNotFoundException() {
        super("Badge template not found");
    }
}
