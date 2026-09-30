package com.correai.api.adapter.in.web.goal;

import com.correai.api.domain.model.goal.GoalPeriod;
import com.correai.api.domain.model.goal.GoalType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record GoalRequest(
        @NotNull GoalType type,
        @NotNull GoalPeriod period,
        @NotNull @DecimalMin("0.01") Double target
) {
}
