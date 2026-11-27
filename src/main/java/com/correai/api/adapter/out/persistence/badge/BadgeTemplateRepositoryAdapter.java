package com.correai.api.adapter.out.persistence.badge;

import com.correai.api.domain.model.badge.BadgeTemplate;
import com.correai.api.domain.model.pagination.PageQuery;
import com.correai.api.domain.model.pagination.PageResult;
import com.correai.api.domain.port.out.badge.BadgeTemplateRepositoryPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import java.util.Optional;
import java.util.UUID;

@Component
public class BadgeTemplateRepositoryAdapter implements BadgeTemplateRepositoryPort {
    private final SpringDataBadgeTemplateRepository repository;

    public BadgeTemplateRepositoryAdapter(SpringDataBadgeTemplateRepository repository) {
        this.repository = repository;
    }

    @Override
    public BadgeTemplate create(BadgeTemplate template) {
        return repository.save(new BadgeTemplateEntity(template)).domain();
    }

    @Override
    public Optional<BadgeTemplate> find(UUID id) {
        return repository.findById(id).map(BadgeTemplateEntity::domain);
    }

    @Override
    public PageResult<BadgeTemplate> list(PageQuery page) {
        var result = repository.findAllByOrderByCreatedAtDescIdAsc(PageRequest.of(page.page(), page.size()));
        return new PageResult<>(result.getContent().stream().map(BadgeTemplateEntity::domain).toList(),
                page.page(), page.size(), result.getTotalElements());
    }
}
