package com.correai.api.application.media;

import com.correai.api.domain.model.activity.ActivityNotFoundException;
import com.correai.api.domain.model.media.MediaContent;
import com.correai.api.domain.model.media.MediaKind;
import com.correai.api.domain.model.media.MediaNotFoundException;
import com.correai.api.domain.port.in.media.MediaUseCase;
import com.correai.api.domain.port.out.activity.ActivityRepositoryPort;
import com.correai.api.domain.port.out.media.MediaRepositoryPort;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class MediaApplicationService implements MediaUseCase {
    private static final int MAX_PHOTO_BYTES = 15 * 1024 * 1024;
    private static final int MAX_GPX_BYTES = 5 * 1024 * 1024;
    private final MediaRepositoryPort media;
    private final ActivityRepositoryPort activities;

    public MediaApplicationService(MediaRepositoryPort media, ActivityRepositoryPort activities) {
        this.media = media;
        this.activities = activities;
    }

    @Override
    public void save(UUID userId, UUID activityId, MediaKind kind, byte[] bytes) {
        checkOwner(userId, activityId, kind);
        if (bytes == null || bytes.length == 0
                || bytes.length > (kind == MediaKind.ACTIVITY_ROUTE ? MAX_GPX_BYTES : MAX_PHOTO_BYTES)) {
            throw new IllegalArgumentException("Invalid file size");
        }
        String contentType = kind == MediaKind.ACTIVITY_ROUTE ? validateGpx(bytes) : validatePhoto(bytes);
        media.save(userId, activityId, kind, new MediaContent(contentType, bytes));
    }

    @Override
    public MediaContent get(UUID userId, UUID activityId, MediaKind kind) {
        checkOwner(userId, activityId, kind);
        return media.find(userId, activityId, kind).orElseThrow(MediaNotFoundException::new);
    }

    @Override
    public void saveActivityPhoto(UUID userId, UUID activityId, UUID photoId, byte[] bytes) {
        checkOwner(userId, activityId, MediaKind.ACTIVITY_PHOTO);
        if (photoId == null || bytes == null || bytes.length == 0 || bytes.length > MAX_PHOTO_BYTES) {
            throw new IllegalArgumentException("Invalid photo");
        }
        media.saveActivityPhoto(userId, activityId, photoId, new MediaContent(validatePhoto(bytes), bytes));
    }

    @Override
    public List<UUID> listActivityPhotos(UUID userId, UUID activityId) {
        checkOwner(userId, activityId, MediaKind.ACTIVITY_PHOTO);
        return media.listActivityPhotos(userId, activityId);
    }

    @Override
    public MediaContent getActivityPhoto(UUID userId, UUID activityId, UUID photoId) {
        checkOwner(userId, activityId, MediaKind.ACTIVITY_PHOTO);
        if (photoId == null) throw new IllegalArgumentException("Invalid photo ID");
        return media.findActivityPhoto(userId, activityId, photoId).orElseThrow(MediaNotFoundException::new);
    }

    private void checkOwner(UUID userId, UUID activityId, MediaKind kind) {
        if (userId == null || kind == null || (kind == MediaKind.USER_PHOTO) != (activityId == null)) {
            throw new IllegalArgumentException("Invalid file owner");
        }

        if (activityId != null && activities.findByIdAndUserId(activityId, userId).isEmpty()) {
            throw new ActivityNotFoundException();
        }
    }

    @Override
    public void deleteActivityPhoto(UUID userId, UUID activityId, UUID photoId) {
        checkOwner(userId, activityId, MediaKind.ACTIVITY_PHOTO);
        if (photoId == null) throw new IllegalArgumentException("Invalid photo ID");
        media.deleteActivityPhoto(userId, activityId, photoId);
    }

    private String validatePhoto(byte[] bytes) {
        try (ImageInputStream input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("Only JPEG and PNG photos are supported");
            }
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName();
                if (!format.equalsIgnoreCase("jpeg") && !format.equalsIgnoreCase("png")) {
                    throw new IllegalArgumentException("Only JPEG and PNG photos are supported");
                }
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > 10000 || height > 10000
                        || (long) width * height > 40_000_000) {
                    throw new IllegalArgumentException("Invalid image dimensions");
                }
                if (reader.read(0) == null) {
                    throw new IllegalArgumentException("Invalid image");
                }
                return format.equalsIgnoreCase("png") ? "image/png" : "image/jpeg";
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid image", e);
        }
    }

    private String validateGpx(byte[] bytes) {
        GpxReader.read(bytes);
        return "application/gpx+xml";
    }
}
