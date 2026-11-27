package com.correai.api.application.media;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class GpxReaderTest {
    private static byte[] gpx(String segments) {
        return ("<gpx xmlns=\"http://www.topografix.com/GPX/1/1\"><trk>" + segments + "</trk></gpx>")
                .getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void preservesOffsetsAndSeparateSegmentsAndAcceptsLegacyWithoutTime() {
        var segments = GpxReader.read(gpx("""
                <trkseg><trkpt lat="0" lon="0"><time>2026-11-27T22:00:00-03:00</time></trkpt>
                <trkpt lat="0" lon="0.1"><time>2026-11-28T01:05:00Z</time></trkpt></trkseg>
                <trkseg><trkpt lat="0" lon="1"/><trkpt lat="0" lon="1.1"/></trkseg>
                """));
        assertEquals(2, segments.size());
        assertEquals(Instant.parse("2026-11-28T01:00:00Z"), segments.getFirst().getFirst().time());
        assertNull(segments.getLast().getFirst().time());
    }

    @Test
    void rejectsLocalInvalidDuplicateAndNonIncreasingTimes() {
        for (String time : new String[]{"2026-11-27T22:00:00", "invalid", "2026-11-27T12:00:00Z"}) {
            assertThrows(IllegalArgumentException.class, () -> GpxReader.read(gpx(
                    "<trkseg><trkpt lat=\"0\" lon=\"0\"><time>2026-11-27T12:00:00Z</time></trkpt>"
                            + "<trkpt lat=\"0\" lon=\"0.1\"><time>" + time + "</time></trkpt></trkseg>")));
        }
        assertThrows(IllegalArgumentException.class, () -> GpxReader.read(gpx("""
                <trkseg><trkpt lat="0" lon="0"><time>2026-11-27T12:00:00Z</time>
                <time>2026-11-27T12:00:01Z</time></trkpt><trkpt lat="0" lon="0.1"/></trkseg>
                """)));
    }

    @Test
    void extensionTimesAndNestedPointsCannotSpoofTiming() {
        var segments = GpxReader.read(gpx("""
                <trkseg><trkpt lat="0" lon="0"><extensions><trkpt>
                <time>2026-11-27T12:00:00Z</time></trkpt></extensions></trkpt>
                <trkpt lat="0" lon="0.1"/></trkseg>
                """));
        assertNull(segments.getFirst().getFirst().time());
        assertEquals(2, segments.getFirst().size());
    }

    @Test
    void rejectsDtdWithoutResolvingEntities() {
        byte[] bytes = """
                <!DOCTYPE gpx [<!ENTITY x SYSTEM "file:///nonexistent">]>
                <gpx xmlns="http://www.topografix.com/GPX/1/1"><trk><trkseg>
                <trkpt lat="0" lon="0"><time>&x;</time></trkpt><trkpt lat="0" lon="1"/>
                </trkseg></trk></gpx>
                """.getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> GpxReader.read(bytes));
    }

    @Test
    void acceptsExactPointLimitAndRejectsOneAdditionalPoint() {
        String point = "<trkpt lat=\"0\" lon=\"0\"/>";
        assertEquals(100_000, GpxReader.read(gpx("<trkseg>" + point.repeat(100_000) + "</trkseg>"))
                .getFirst().size());
        assertThrows(IllegalArgumentException.class,
                () -> GpxReader.read(gpx("<trkseg>" + point.repeat(100_001) + "</trkseg>")));
    }
}
