package com.correai.api.domain.model.pagination;

public record PageQuery(int page, int size) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public PageQuery {
        if (page < 0) {
            throw new IllegalArgumentException("Page must not be negative");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("Size must be between 1 and " + MAX_SIZE);
        }
    }
}
