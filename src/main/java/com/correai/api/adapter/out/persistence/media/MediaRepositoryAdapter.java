package com.correai.api.adapter.out.persistence.media;

import com.correai.api.domain.model.media.MediaContent;
import com.correai.api.domain.model.media.MediaKind;
import com.correai.api.domain.port.out.media.MediaRepositoryPort;
import org.springframework.stereotype.Component;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Component
public class MediaRepositoryAdapter implements MediaRepositoryPort {
    private final SpringDataMediaRepository repository;

    public MediaRepositoryAdapter(SpringDataMediaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(UUID userId, UUID activityId, MediaKind kind, MediaContent content) {
        repository.save(new MediaEntity(key(userId, activityId, kind), userId, activityId,
                content.contentType(), content.bytes()));
    }

    @Override
    public Optional<MediaContent> find(UUID userId, UUID activityId, MediaKind kind) {
        return repository.findByIdAndUserId(key(userId, activityId, kind), userId)
                .map(entity -> new MediaContent(entity.contentType(), entity.content()));
    }

    private String key(UUID userId, UUID activityId, MediaKind kind) {
        return kind.name() + ":" + (activityId == null ? userId : activityId);
    }

    @Override
    public void saveActivityPhoto(UUID userId, UUID activityId, UUID photoId, MediaContent content) {
        repository.save(new MediaEntity(photoKey(activityId, photoId), userId, activityId,
                content.contentType(), content.bytes()));
    }

    @Override
    public List<UUID> listActivityPhotos(UUID userId, UUID activityId) {
        String legacy = key(userId, activityId, MediaKind.ACTIVITY_PHOTO);
        return repository.findPhotoIds(userId, activityId, legacy, legacy + ":%").stream()
                .map(id -> UUID.fromString(id.substring(id.lastIndexOf(':') + 1)))
                .toList();
    }

    @Override
    public Optional<MediaContent> findActivityPhoto(UUID userId, UUID activityId, UUID photoId) {
        return repository.findByIdAndUserId(photoKey(activityId, photoId), userId)
                .map(entity -> new MediaContent(entity.contentType(), entity.content()));
    }

    private String photoKey(UUID activityId, UUID photoId) {
        String legacy = key(null, activityId, MediaKind.ACTIVITY_PHOTO);
        return activityId.equals(photoId) ? legacy : legacy + ":" + photoId;
    }

    @Override
    public void deleteActivityPhoto(UUID userId, UUID activityId, UUID photoId) {
        repository.deletePhoto(photoKey(activityId, photoId), userId);
    }

    @Override
    public List<UUID> listRouteActivityIds(UUID userId) {
        return repository.findRouteActivityIds(userId);
    }
}
