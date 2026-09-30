package com.correai.api.adapter.out.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataUserRepository extends JpaRepository<UserEntity, UUID> {
}

