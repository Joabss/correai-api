package com.correai.api.domain.model.activity;

import java.time.Instant;

public record TimedRoutePoint(double latitude, double longitude, Instant time) {
    public TimedRoutePoint {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                || Math.abs(latitude) > 90 || Math.abs(longitude) > 180) {
            throw new IllegalArgumentException("Invalid route coordinates");
        }
    }
}
