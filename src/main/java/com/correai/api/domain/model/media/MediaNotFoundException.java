package com.correai.api.domain.model.media;

public class MediaNotFoundException extends RuntimeException {
    public MediaNotFoundException() {
        super("File not found");
    }
}
