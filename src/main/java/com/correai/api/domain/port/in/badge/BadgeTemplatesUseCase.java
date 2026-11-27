package com.correai.api.domain.port.in.badge;

import com.correai.api.domain.model.badge.BadgeIcon;
import com.correai.api.domain.model.badge.BadgeTemplate;
import com.correai.api.domain.model.pagination.PageQuery;
import com.correai.api.domain.model.pagination.PageResult;
import java.util.UUID;

public interface BadgeTemplatesUseCase {
    BadgeTemplate create(UUID actor, String name, String description, BadgeIcon icon, String color);
    BadgeTemplate get(UUID id);
    PageResult<BadgeTemplate> list(PageQuery page);
}
