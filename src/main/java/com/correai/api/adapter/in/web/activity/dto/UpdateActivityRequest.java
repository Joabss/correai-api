package com.correai.api.adapter.in.web.activity.dto;

import com.correai.api.domain.model.activity.ActivityType;
import com.correai.api.domain.model.activity.PerceivedEffort;
import com.correai.api.domain.model.activity.TrainingType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateActivityRequest(
        @NotNull ActivityType type,
        @NotNull @DecimalMin("0.01") Double distanceKm,
        @NotNull @Positive Integer durationSeconds,
        TrainingType trainingType,
        PerceivedEffort perceivedEffort,
        @Size(max = 255) String notes
) {
}
