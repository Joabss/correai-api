package com.correai.api.adapter.out.persistence.badge;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

interface SpringDataBadgeTemplateRepository extends JpaRepository<BadgeTemplateEntity, UUID> {
    Page<BadgeTemplateEntity> findAllByOrderByCreatedAtDescIdAsc(Pageable page);
}
