package com.correai.api.application.stats;

import com.correai.api.domain.model.media.MediaContent;
import com.correai.api.domain.model.media.MediaKind;
import com.correai.api.domain.port.out.media.MediaRepositoryPort;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PersonalRecordApplicationServiceTest {
    private static MediaContent route(int seconds, boolean timed) {
        String time = timed ? "<time>2026-11-27T12:00:00Z</time>" : "";
        String finish = timed ? "<time>" + java.time.Instant.parse("2026-11-27T12:00:00Z").plusSeconds(seconds) + "</time>" : "";
        String xml = "<gpx xmlns=\"http://www.topografix.com/GPX/1/1\"><trk><trkseg>"
                + "<trkpt lat=\"0\" lon=\"0\">" + time + "</trkpt>"
                + "<trkpt lat=\"0\" lon=\"0.5\">" + finish + "</trkpt></trkseg></trk></gpx>";
        return new MediaContent("application/gpx+xml", xml.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void returnsTopThreeDistinctActivitiesForAllOfficialDistancesWithDeterministicTies() {
        MediaRepositoryPort media = mock(MediaRepositoryPort.class);
        UUID user = UUID.randomUUID();
        UUID a = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID b = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID c = UUID.fromString("00000000-0000-0000-0000-000000000003");
        UUID d = UUID.fromString("00000000-0000-0000-0000-000000000004");
        UUID legacy = UUID.randomUUID();
        when(media.listRouteActivityIds(user)).thenReturn(List.of(d, c, b, a, legacy));
        when(media.find(user, a, MediaKind.ACTIVITY_ROUTE)).thenReturn(Optional.of(route(1000, true)));
        when(media.find(user, b, MediaKind.ACTIVITY_ROUTE)).thenReturn(Optional.of(route(1000, true)));
        when(media.find(user, c, MediaKind.ACTIVITY_ROUTE)).thenReturn(Optional.of(route(1200, true)));
        when(media.find(user, d, MediaKind.ACTIVITY_ROUTE)).thenReturn(Optional.of(route(1400, true)));
        when(media.find(user, legacy, MediaKind.ACTIVITY_ROUTE)).thenReturn(Optional.of(route(100, false)));
        var records = new PersonalRecordApplicationService(media).records(user);
        assertEquals(List.of(5.0, 10.0, 21.0975, 42.195), records.stream().map(r -> r.distanceKm()).toList());
        for (var record : records) {
            assertEquals(List.of(a, b, c), record.efforts().stream().map(e -> e.activityId()).toList());
        }
        verify(media).listRouteActivityIds(user);
    }

    @Test
    void ignoresCorruptedOrLegacyRouteWithNonIncreasingTimesWithoutFailingOtherActivities() {
        MediaRepositoryPort media = mock(MediaRepositoryPort.class);
        UUID user = UUID.randomUUID();
        UUID valid = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID corrupted = UUID.randomUUID();
        when(media.listRouteActivityIds(user)).thenReturn(List.of(corrupted, valid));
        when(media.find(user, valid, MediaKind.ACTIVITY_ROUTE)).thenReturn(Optional.of(route(1000, true)));
        String xml = "<gpx xmlns=\"http://www.topografix.com/GPX/1/1\"><trk><trkseg>"
                + "<trkpt lat=\"0\" lon=\"0\"><time>2026-11-27T12:00:05Z</time></trkpt>"
                + "<trkpt lat=\"0\" lon=\"0.5\"><time>2026-11-27T12:00:00Z</time></trkpt></trkseg></trk></gpx>";
        when(media.find(user, corrupted, MediaKind.ACTIVITY_ROUTE)).thenReturn(Optional.of(
                new MediaContent("application/gpx+xml", xml.getBytes(StandardCharsets.UTF_8))));
        var records = new PersonalRecordApplicationService(media).records(user);
        for (var record : records) {
            assertEquals(List.of(valid), record.efforts().stream().map(e -> e.activityId()).toList());
        }
    }

    @Test
    void doesNotJoinShortSegmentsAcrossPauses() {
        MediaRepositoryPort media = mock(MediaRepositoryPort.class);
        UUID user = UUID.randomUUID();
        UUID activity = UUID.randomUUID();
        when(media.listRouteActivityIds(user)).thenReturn(List.of(activity));
        String segment = "<trkseg><trkpt lat=\"0\" lon=\"0\"><time>2026-11-27T12:00:00Z</time></trkpt>"
                + "<trkpt lat=\"0\" lon=\"0.03\"><time>2026-11-27T12:05:00Z</time></trkpt></trkseg>";
        when(media.find(user, activity, MediaKind.ACTIVITY_ROUTE)).thenReturn(Optional.of(new MediaContent("application/gpx+xml",
                ("<gpx xmlns=\"http://www.topografix.com/GPX/1/1\"><trk>" + segment + segment + "</trk></gpx>")
                        .getBytes(StandardCharsets.UTF_8))));
        assertTrue(new PersonalRecordApplicationService(media).records(user).stream().allMatch(r -> r.efforts().isEmpty()));
    }
}
