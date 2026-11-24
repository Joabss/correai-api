package com.correai.api.adapter.out.persistence.token;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataOneTimeTokenRepository extends JpaRepository<OneTimeTokenEntity, UUID> {

    Optional<OneTimeTokenEntity> findByTokenHashAndType(String tokenHash, String type);

    @Modifying
    @Query("delete from OneTimeTokenEntity t where t.id = :id")
    int deleteByIdReturningCount(@Param("id") UUID id);

    @Modifying
    @Query("delete from OneTimeTokenEntity t where t.userId = :userId and t.type = :type")
    int deleteByUserIdAndType(@Param("userId") UUID userId, @Param("type") String type);
}
