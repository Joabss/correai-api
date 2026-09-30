package com.correai.api.adapter.out.persistence.activity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SpringDataActivityRepository extends JpaRepository<ActivityEntity, UUID> {

    List<ActivityEntity> findByUserIdAndActivityDateBetween(UUID userId, LocalDate start, LocalDate end);

    boolean existsByUserIdAndActivityDate(UUID userId, LocalDate date);

    @Query("select max(a.distanceKm) from ActivityEntity a where a.userId = :userId")
    Double findLongestDistance(@Param("userId") UUID userId);

    Page<ActivityEntity> findByUserId(UUID userId, Pageable pageable);
}

