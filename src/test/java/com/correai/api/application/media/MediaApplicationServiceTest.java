package com.correai.api.application.media;

import com.correai.api.domain.model.activity.Activity;
import com.correai.api.domain.model.activity.ActivityNotFoundException;
import com.correai.api.domain.model.media.MediaContent;
import com.correai.api.domain.model.media.MediaKind;
import com.correai.api.domain.model.media.MediaNotFoundException;
import com.correai.api.domain.port.out.activity.ActivityRepositoryPort;
import com.correai.api.domain.port.out.media.MediaRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Arrays;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaApplicationServiceTest {
    @Mock MediaRepositoryPort media;
    @Mock ActivityRepositoryPort activities;
    @Mock Activity activity;
    MediaApplicationService service;
    UUID user = UUID.randomUUID();
    UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new MediaApplicationService(media, activities);
    }

    @Test
    void savesValidPngPhoto() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", output);
        byte[] image = output.toByteArray();
        service.save(user, null, MediaKind.USER_PHOTO, image);
        verify(media).save(eq(user), isNull(), eq(MediaKind.USER_PHOTO),
                argThat(content -> content.contentType().equals("image/png")
                        && java.util.Arrays.equals(content.bytes(), image)));
    }

    @Test
    void rejectsInvalidAndOversizedPhotos() {
        assertThrows(IllegalArgumentException.class,
                () -> service.save(user, null, MediaKind.USER_PHOTO, new byte[] {1, 2}));
        assertThrows(IllegalArgumentException.class,
                () -> service.save(user, null, MediaKind.USER_PHOTO, new byte[15 * 1024 * 1024 + 1]));
        verifyNoInteractions(media);
    }

    @Test
    void acceptsPhotoAtExactSizeLimit() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", output);
        byte[] image = Arrays.copyOf(output.toByteArray(), 15 * 1024 * 1024);
        service.save(user, null, MediaKind.USER_PHOTO, image);
        verify(media).save(eq(user), isNull(), eq(MediaKind.USER_PHOTO), any());
    }

    @Test
    void enforcesGpxSizeLimit() {
        when(activities.findByIdAndUserId(id, user)).thenReturn(Optional.of(activity));
        String base = "<gpx xmlns=\"http://www.topografix.com/GPX/1/1\">"
                + "<rte><rtept lat=\"0\" lon=\"0\"/><rtept lat=\"1\" lon=\"1\"/></rte></gpx>";
        byte[] gpx = Arrays.copyOf(base.getBytes(StandardCharsets.UTF_8), 5 * 1024 * 1024);
        Arrays.fill(gpx, base.length(), gpx.length, (byte) ' ');
        service.save(user, id, MediaKind.ACTIVITY_ROUTE, gpx);
        assertThrows(IllegalArgumentException.class,
                () -> service.save(user, id, MediaKind.ACTIVITY_ROUTE, Arrays.copyOf(gpx, gpx.length + 1)));
        verify(media, times(1)).save(eq(user), eq(id), eq(MediaKind.ACTIVITY_ROUTE), any());
    }

    @Test
    void savesValidGpxAndReturnsOriginalBytes() {
        byte[] gpx = ("<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" version=\"1.1\" creator=\"test\">"
                + "<trk><trkseg><trkpt lat=\"-23.5\" lon=\"-46.6\"/>"
                + "<trkpt lat=\"-23.6\" lon=\"-46.7\"/></trkseg></trk></gpx>")
                .getBytes(StandardCharsets.UTF_8);
        when(activities.findByIdAndUserId(id, user)).thenReturn(Optional.of(activity));
        service.save(user, id, MediaKind.ACTIVITY_ROUTE, gpx);
        verify(media).save(eq(user), eq(id), eq(MediaKind.ACTIVITY_ROUTE),
                argThat(content -> content.contentType().equals("application/gpx+xml")
                        && java.util.Arrays.equals(content.bytes(), gpx)));
        when(media.find(user, id, MediaKind.ACTIVITY_ROUTE))
                .thenReturn(Optional.of(new MediaContent("application/gpx+xml", gpx)));
        assertArrayEquals(gpx, service.get(user, id, MediaKind.ACTIVITY_ROUTE).bytes());
    }

    @Test
    void rejectsGpxWithInvalidCoordinatesOrExternalEntities() {
        when(activities.findByIdAndUserId(id, user)).thenReturn(Optional.of(activity));
        String invalid = "<gpx xmlns=\"http://www.topografix.com/GPX/1/1\">"
                + "<trkpt lat=\"91\" lon=\"0\"/><trkpt lat=\"0\" lon=\"0\"/></gpx>";
        String xxe = "<!DOCTYPE gpx [<!ENTITY xxe SYSTEM \"file:///secret\">]>"
                + "<gpx xmlns=\"http://www.topografix.com/GPX/1/1\">&xxe;</gpx>";
        assertThrows(IllegalArgumentException.class, () -> service.save(user, id, MediaKind.ACTIVITY_ROUTE,
                invalid.getBytes(StandardCharsets.UTF_8)));
        assertThrows(IllegalArgumentException.class, () -> service.save(user, id, MediaKind.ACTIVITY_ROUTE,
                xxe.getBytes(StandardCharsets.UTF_8)));
        verifyNoInteractions(media);
    }

    @Test
    void rejectsAccessToAnotherUsersActivity() {
        when(activities.findByIdAndUserId(id, user)).thenReturn(Optional.empty());
        assertThrows(ActivityNotFoundException.class, () -> service.get(user, id, MediaKind.ACTIVITY_PHOTO));
        assertThrows(ActivityNotFoundException.class,
                () -> service.save(user, id, MediaKind.ACTIVITY_ROUTE, new byte[] {1}));
        verifyNoInteractions(media);
    }

    @Test
    void missingMediaIsNotFound() {
        assertThrows(MediaNotFoundException.class, () -> service.get(user, null, MediaKind.USER_PHOTO));
    }
}
