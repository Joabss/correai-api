package com.correai.api.adapter.out.persistence.media;

import com.correai.api.domain.model.media.MediaContent;
import com.correai.api.domain.model.media.MediaKind;
import com.correai.api.domain.port.out.media.MediaRepositoryPort;
import org.springframework.stereotype.Component;
import java.util.Optional;
import java.util.UUID;

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
}
