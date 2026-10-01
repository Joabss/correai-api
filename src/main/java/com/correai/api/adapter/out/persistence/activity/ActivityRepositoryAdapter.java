package com.correai.api.adapter.out.persistence.activity;

import com.correai.api.domain.model.activity.Activity;
import com.correai.api.domain.model.pagination.PageQuery;
import com.correai.api.domain.model.pagination.PageResult;
import com.correai.api.domain.port.out.activity.ActivityRepositoryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ActivityRepositoryAdapter implements ActivityRepositoryPort {

    private final SpringDataActivityRepository jpaRepository;
    private final ActivityMapper mapper;

    public ActivityRepositoryAdapter(SpringDataActivityRepository jpaRepository, ActivityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Activity save(Activity activity) {
        ActivityEntity saved = jpaRepository.save(mapper.toEntity(activity));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Activity> findByIdAndUserId(UUID id, UUID userId) {
        return jpaRepository.findByIdAndUserId(id, userId).map(mapper::toDomain);
    }

    @Override
    public void delete(Activity activity) {
        jpaRepository.deleteById(activity.id());
    }

    @Override
    public PageResult<Activity> findByUserIdOrderByActivityDateDesc(UUID userId, PageQuery pageQuery) {
        Pageable pageable = PageRequest.of(pageQuery.page(), pageQuery.size(),
                Sort.by(Sort.Order.desc("activityDate"), Sort.Order.desc("createdAt")));
        Page<ActivityEntity> result = jpaRepository.findByUserId(userId, pageable);
        return new PageResult<>(
                result.getContent().stream().map(mapper::toDomain).toList(),
                pageQuery.page(),
                pageQuery.size(),
                result.getTotalElements());
    }

    @Override
    public List<Activity> findByUserIdAndActivityDateBetween(UUID userId, LocalDate start, LocalDate end) {
        return jpaRepository.findByUserIdAndActivityDateBetween(userId, start, end)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByUserIdAndActivityDate(UUID userId, LocalDate date) {
        return jpaRepository.existsByUserIdAndActivityDate(userId, date);
    }

    @Override
    public Double findLongestDistance(UUID userId) {
        return jpaRepository.findLongestDistance(userId);
    }
}

