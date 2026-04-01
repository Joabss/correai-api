package com.correai.api.dto;

import com.correai.api.domain.activity.ActivityType;
import com.correai.api.domain.activity.PerceivedEffort;
import com.correai.api.domain.activity.TrainingType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActivityRequest {

    @NotNull
    private ActivityType type;

    @NotNull
    @DecimalMin("0.01")
    private Double distanceKm;

    @NotNull
    private Integer durationSeconds;

    private TrainingType trainingType;
    private PerceivedEffort perceivedEffort;
    private String notes;
}
