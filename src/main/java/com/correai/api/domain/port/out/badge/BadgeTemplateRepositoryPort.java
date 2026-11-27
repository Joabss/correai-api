package com.correai.api.domain.port.out.badge;

import com.correai.api.domain.model.badge.BadgeTemplate;
import com.correai.api.domain.model.pagination.PageQuery;
import com.correai.api.domain.model.pagination.PageResult;
import java.util.Optional;
import java.util.UUID;

public interface BadgeTemplateRepositoryPort {
    BadgeTemplate create(BadgeTemplate template);
    Optional<BadgeTemplate> find(UUID id);
    PageResult<BadgeTemplate> list(PageQuery page);
}
