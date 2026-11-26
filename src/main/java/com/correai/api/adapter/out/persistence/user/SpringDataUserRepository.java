package com.correai.api.adapter.out.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataUserRepository extends JpaRepository<UserEntity, UUID> {

    boolean existsByEmail(String email);

    Optional<UserEntity> findByEmail(String email);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("update UserEntity u set u.name = :name, u.email = :email where u.id = :id "
            + "and (u.email = :expectedEmail or (u.email is null and :expectedEmail is null))")
    int updateProfile(@Param("id") UUID id, @Param("name") String name, @Param("email") String email,
                      @Param("expectedEmail") String expectedEmail);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("update UserEntity u set u.passwordHash = :passwordHash where u.id = :id")
    int updatePasswordHash(@Param("id") UUID id, @Param("passwordHash") String passwordHash);
}
