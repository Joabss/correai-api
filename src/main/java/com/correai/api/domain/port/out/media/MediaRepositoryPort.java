package com.correai.api.domain.port.out.media;

import com.correai.api.domain.model.media.MediaContent;
import com.correai.api.domain.model.media.MediaKind;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface MediaRepositoryPort {
    void save(UUID userId, UUID activityId, MediaKind kind, MediaContent content);
    Optional<MediaContent> find(UUID userId, UUID activityId, MediaKind kind);
    void saveActivityPhoto(UUID userId, UUID activityId, UUID photoId, MediaContent content);
    List<UUID> listActivityPhotos(UUID userId, UUID activityId);
    Optional<MediaContent> findActivityPhoto(UUID userId, UUID activityId, UUID photoId);
    void deleteActivityPhoto(UUID userId, UUID activityId, UUID photoId);
    List<UUID> listRouteActivityIds(UUID userId);
}
