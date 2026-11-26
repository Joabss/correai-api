package com.correai.api.domain.port.in.media;

import com.correai.api.domain.model.media.MediaContent;
import com.correai.api.domain.model.media.MediaKind;
import java.util.UUID;

public interface MediaUseCase {
    void save(UUID userId, UUID activityId, MediaKind kind, byte[] bytes);
    MediaContent get(UUID userId, UUID activityId, MediaKind kind);
}
