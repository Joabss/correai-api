package com.correai.api.adapter.out.persistence.media;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataMediaRepository extends JpaRepository<MediaEntity, String> {
    Optional<MediaEntity> findByIdAndUserId(String id, UUID userId);
}
