package com.correai.api.adapter.out.persistence.media;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

public interface SpringDataMediaRepository extends JpaRepository<MediaEntity, String> {
    Optional<MediaEntity> findByIdAndUserId(String id, UUID userId);

    @Query("select m.id from MediaEntity m where m.userId = :userId and m.activityId = :activityId "
            + "and (m.id = :legacyKey or m.id like :photoPrefix) order by m.id")
    List<String> findPhotoIds(UUID userId, UUID activityId, String legacyKey, String photoPrefix);

    @Modifying
    @Transactional
    @Query("delete from MediaEntity m where m.id = :id and m.userId = :userId")
    void deletePhoto(String id, UUID userId);

    @Query("select m.activityId from MediaEntity m join ActivityEntity a "
            + "on a.id = m.activityId and a.userId = m.userId where m.userId = :userId "
            + "and m.id like 'ACTIVITY_ROUTE:%' and m.activityId is not null order by m.activityId")
    List<UUID> findRouteActivityIds(UUID userId);
}
