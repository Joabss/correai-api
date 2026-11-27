package com.correai.api.domain.port.in.activity;

import com.correai.api.domain.model.activity.Activity;

public interface UpdateActivityUseCase {
    Activity update(UpdateActivityCommand command);
}
