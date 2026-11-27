package com.correai.api.domain.port.in.media;

import com.correai.api.domain.model.media.MediaContent;
import com.correai.api.domain.model.media.MediaKind;
import java.util.UUID;
import java.util.List;

public interface MediaUseCase {
    void save(UUID userId, UUID activityId, MediaKind kind, byte[] bytes);
    MediaContent get(UUID userId, UUID activityId, MediaKind kind);
    void saveActivityPhoto(UUID userId, UUID activityId, UUID photoId, byte[] bytes);
    List<UUID> listActivityPhotos(UUID userId, UUID activityId);
    MediaContent getActivityPhoto(UUID userId, UUID activityId, UUID photoId);
    void deleteActivityPhoto(UUID userId, UUID activityId, UUID photoId);
}
