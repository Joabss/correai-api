package com.correai.api.domain.port.in.activity;

import com.correai.api.domain.model.activity.Activity;
import com.correai.api.domain.model.pagination.PageQuery;
import com.correai.api.domain.model.pagination.PageResult;

import java.util.UUID;

/**
 * Inbound port (use case): list activities for a user.
 */
public interface ListActivitiesUseCase {

    PageResult<Activity> list(UUID userId, PageQuery pageQuery);
}

