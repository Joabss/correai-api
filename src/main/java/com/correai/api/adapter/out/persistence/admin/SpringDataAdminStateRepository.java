package com.correai.api.adapter.out.persistence.admin;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

interface SpringDataAdminStateRepository extends JpaRepository<AdminStateEntity, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from AdminStateEntity s where s.id = 1")
    Optional<AdminStateEntity> lockState();
}
