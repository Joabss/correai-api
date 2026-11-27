package com.correai.api.adapter.out.persistence.challenge;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

interface SpringDataChallengeRepository extends JpaRepository<ChallengeEntity, UUID> {
    String VISIBLE = """
            from ChallengeEntity c
            where c.publishedAt is not null and
              (c.creatorId = :viewer or
                (not exists (select b.id from UserBlockEntity b where
                   (b.blockerId = :viewer and b.blockedId = c.creatorId) or
                   (b.blockerId = c.creatorId and b.blockedId = :viewer))
                 and (c.audience = com.correai.api.domain.model.challenge.ChallengeAudience.GLOBAL
                   or (c.audience = com.correai.api.domain.model.challenge.ChallengeAudience.FRIENDS
                     and exists (select f.id from FriendshipEntity f
                       where f.state = com.correai.api.domain.model.friendship.FriendshipState.ACCEPTED
                         and ((f.requesterId = :viewer and f.recipientId = c.creatorId)
                           or (f.recipientId = :viewer and f.requesterId = c.creatorId))))
                   or (c.audience = com.correai.api.domain.model.challenge.ChallengeAudience.GROUP
                     and exists (select m.id from GroupMemberEntity m
                       where m.groupId = c.groupId and m.userId = :viewer)))))
            """;

    Optional<ChallengeEntity> findByIdAndCreatorId(UUID id, UUID creatorId);
    Page<ChallengeEntity> findByCreatorIdAndPublishedAtIsNullOrderByCreatedAtDescIdAsc(UUID creatorId, Pageable page);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ChallengeEntity c where c.id = :id and c.creatorId = :creator")
    Optional<ChallengeEntity> lockOwn(@Param("id") UUID id, @Param("creator") UUID creator);

    @Query("select c " + VISIBLE + " and c.id = :id")
    Optional<ChallengeEntity> findVisible(@Param("viewer") UUID viewer, @Param("id") UUID id);

    @Query(value = "select c " + VISIBLE + " order by c.publishedAt desc, c.id asc",
            countQuery = "select count(c) " + VISIBLE)
    Page<ChallengeEntity> listVisible(@Param("viewer") UUID viewer, Pageable page);
}
