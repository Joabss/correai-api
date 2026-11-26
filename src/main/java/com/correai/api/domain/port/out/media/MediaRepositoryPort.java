package com.correai.api.domain.port.out.media;

import com.correai.api.domain.model.media.MediaContent;
import com.correai.api.domain.model.media.MediaKind;
import java.util.Optional;
import java.util.UUID;

public interface MediaRepositoryPort {
    void save(UUID userId, UUID activityId, MediaKind kind, MediaContent content);
    Optional<MediaContent> find(UUID userId, UUID activityId, MediaKind kind);
}
