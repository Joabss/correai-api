package com.correai.api.domain.port.out.activity;

import com.correai.api.domain.model.activity.Activity;
import com.correai.api.domain.model.pagination.PageQuery;
import com.correai.api.domain.model.pagination.PageResult;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Outbound port for Activity persistence, implemented by an infrastructure adapter.
 */
public interface ActivityRepositoryPort {

    Activity save(Activity activity);

    PageResult<Activity> findByUserIdOrderByActivityDateDesc(UUID userId, PageQuery pageQuery);

    List<Activity> findByUserIdAndActivityDateBetween(UUID userId, LocalDate start, LocalDate end);

    boolean existsByUserIdAndActivityDate(UUID userId, LocalDate date);

    Double findLongestDistance(UUID userId);
}

