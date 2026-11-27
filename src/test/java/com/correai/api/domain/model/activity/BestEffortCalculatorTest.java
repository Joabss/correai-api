package com.correai.api.domain.model.activity;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class BestEffortCalculatorTest {
    private static TimedRoutePoint point(double meters, int seconds) {
        return new TimedRoutePoint(0, Math.toDegrees(meters / 6371000),
                Instant.parse("2026-11-27T12:00:00Z").plusSeconds(seconds));
    }

    @Test
    void interpolatesFinishBetweenPointsAndFindsFastestStart() {
        var points = List.of(point(0, 0), point(1000, 1000), point(7000, 1600));
        assertEquals(500, BestEffortCalculator.fastestSeconds(points, 5000).orElseThrow(), 1e-6);
    }

    @Test
    void optimumCanStartBetweenRecordedPoints() {
        var points = List.of(point(0, 0), point(4000, 400), point(5000, 410), point(9000, 4410));
        assertEquals(410, BestEffortCalculator.fastestSeconds(points, 5000).orElseThrow(), 1e-6);
        assertEquals(360, BestEffortCalculator.fastestSeconds(points, 4500).orElseThrow(), 1e-6);
    }

    @Test
    void exactThresholdsDoNotUseDistanceToleranceOrExtrapolation() {
        for (double meters : new double[]{5000, 10000, 21097.5, 42195}) {
            assertTrue(BestEffortCalculator.fastestSeconds(List.of(point(0, 0), point(meters - .01, 1000)), meters).isEmpty());
            assertEquals(1000 * meters / (meters + .01),
                    BestEffortCalculator.fastestSeconds(List.of(point(0, 0), point(meters + .01, 1000)), meters).orElseThrow(), 1e-6);
        }
    }

    @Test
    void excludesUntimedRoutesAndRejectsNonIncreasingTimes() {
        assertTrue(BestEffortCalculator.fastestSeconds(List.of(point(0, 0),
                new TimedRoutePoint(0, .1, null)), 5000).isEmpty());
        assertThrows(IllegalArgumentException.class, () ->
                BestEffortCalculator.fastestSeconds(List.of(point(0, 0), point(6000, 0)), 5000));
    }

    @Test
    void stationaryTimeInsideAnEffortCountsButStartAndFinishCanExcludeIt() {
        var points = List.of(point(0, 0), point(0, 100), point(2000, 300),
                point(2000, 400), point(6000, 800), point(6000, 900));
        assertEquals(600, BestEffortCalculator.fastestSeconds(points, 5000).orElseThrow(), 1e-6);
        assertEquals(400, BestEffortCalculator.fastestSeconds(points, 4000).orElseThrow(), 1e-6);
    }
}
