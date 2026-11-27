package com.correai.api.domain.model.activity;

import java.time.Duration;
import java.util.List;
import java.util.OptionalDouble;

public final class BestEffortCalculator {
    private BestEffortCalculator() {
    }

    public static OptionalDouble fastestSeconds(List<TimedRoutePoint> points, double meters) {
        if (!Double.isFinite(meters) || meters <= 0) throw new IllegalArgumentException("Invalid distance");
        if (points.size() < 2 || points.stream().anyMatch(p -> p.time() == null)) return OptionalDouble.empty();
        double[] distance = new double[points.size()];
        double[] time = new double[points.size()];
        for (int i = 1; i < points.size(); i++) {
            if (!points.get(i).time().isAfter(points.get(i - 1).time())) {
                throw new IllegalArgumentException("Route times must be strictly increasing");
            }
            distance[i] = distance[i - 1] + distanceMeters(points.get(i - 1), points.get(i));
            Duration elapsed = Duration.between(points.getFirst().time(), points.get(i).time());
            time[i] = elapsed.getSeconds() + elapsed.getNano() / 1e9;
        }
        double total = distance[distance.length - 1];
        if (total < meters) return OptionalDouble.empty();
        double best = Double.POSITIVE_INFINITY;
        // With linear interpolation, an optimum has a recorded point at either boundary.
        int end = 1;
        for (int start = 0; start < distance.length && distance[start] + meters <= total; start++) {
            double target = distance[start] + meters;
            while (distance[end] < target) end++;
            best = Math.min(best, interpolate(distance, time, end - 1, end, target) - time[start]);
        }
        int start = 0;
        for (end = 1; end < distance.length; end++) {
            double target = distance[end] - meters;
            if (target < 0) continue;
            while (start + 1 < distance.length && distance[start + 1] <= target) start++;
            best = Math.min(best, time[end] - interpolate(distance, time, start, start + 1, target));
        }
        return OptionalDouble.of(best);
    }

    private static double interpolate(double[] distance, double[] time, int a, int b, double target) {
        if (distance[a] == target) return time[a];
        if (distance[b] == target) return time[b];
        return time[a] + (time[b] - time[a]) * (target - distance[a]) / (distance[b] - distance[a]);
    }

    public static double distanceMeters(TimedRoutePoint a, TimedRoutePoint b) {
        double latA = Math.toRadians(a.latitude());
        double latB = Math.toRadians(b.latitude());
        double dLat = latB - latA;
        double dLon = Math.toRadians(b.longitude() - a.longitude());
        double h = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(latA) * Math.cos(latB) * Math.pow(Math.sin(dLon / 2), 2);
        return 2 * 6371000 * Math.asin(Math.sqrt(Math.clamp(h, 0, 1)));
    }
}
