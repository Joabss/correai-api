package com.correai.api.application.badge;

import com.correai.api.domain.model.badge.BadgeIcon;
import com.correai.api.domain.model.badge.BadgeTemplate;
import com.correai.api.domain.model.badge.BadgeTemplateNotFoundException;
import com.correai.api.domain.model.pagination.PageQuery;
import com.correai.api.domain.model.pagination.PageResult;
import com.correai.api.domain.port.in.badge.BadgeTemplatesUseCase;
import com.correai.api.domain.port.out.badge.BadgeTemplateRepositoryPort;
import com.correai.api.domain.port.in.admin.AdminUseCase;
import com.correai.api.domain.model.admin.AdminAccessDeniedException;
import java.time.Clock;
import java.util.UUID;

public class BadgeTemplateApplicationService implements BadgeTemplatesUseCase {
    private final BadgeTemplateRepositoryPort templates;
    private final Clock clock;
    private final AdminUseCase admins;

    public BadgeTemplateApplicationService(BadgeTemplateRepositoryPort templates, AdminUseCase admins, Clock clock) {
        this.templates = templates;
        this.admins = admins;
        this.clock = clock;
    }

    @Override
    public BadgeTemplate create(UUID actor, String name, String description, BadgeIcon icon, String color) {
        if (!admins.isAdmin(actor)) throw new AdminAccessDeniedException();
        return templates.create(new BadgeTemplate(UUID.randomUUID(), name, description, icon, color,
                actor, clock.instant()));
    }

    @Override
    public BadgeTemplate get(UUID id) {
        return templates.find(id).orElseThrow(BadgeTemplateNotFoundException::new);
    }

    @Override
    public PageResult<BadgeTemplate> list(PageQuery page) {
        return templates.list(page);
    }
}
