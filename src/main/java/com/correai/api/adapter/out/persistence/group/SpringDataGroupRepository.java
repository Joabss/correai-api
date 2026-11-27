package com.correai.api.adapter.out.persistence.group;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

interface SpringDataGroupRepository extends JpaRepository<GroupEntity, UUID> {
    Page<GroupEntity> findByNameContainingIgnoreCaseOrderByNameAscIdAsc(String name, Pageable page);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from GroupEntity g where g.id = :id")
    Optional<GroupEntity> lockById(@Param("id") UUID id);
}
