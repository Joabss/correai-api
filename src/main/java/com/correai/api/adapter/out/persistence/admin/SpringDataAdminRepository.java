package com.correai.api.adapter.out.persistence.admin;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

interface SpringDataAdminRepository extends JpaRepository<AdminEntity, UUID> {}
