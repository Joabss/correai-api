package com.correai.api.repository;

import com.correai.api.domain.activity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ActivityRepository extends JpaRepository<Activity, UUID> {

    List<Activity> findByUserIdAndActivityDateBetween(UUID userId, LocalDate start, LocalDate end);

    boolean existsByUserIdAndActivityDate(UUID userId, LocalDate date);

    @Query("select max(a.distanceKm) from Activity a where a.userId = :userId")
    Double findLongestDistance(@Param("userId") UUID userId);

    List<Activity> findByUserIdOrderByActivityDateDesc(UUID userId);

}
