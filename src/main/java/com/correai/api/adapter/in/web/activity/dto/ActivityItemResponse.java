package com.correai.api.adapter.in.web.activity.dto;

import com.correai.api.domain.model.activity.ActivityType;
import com.correai.api.domain.model.activity.PerceivedEffort;
import com.correai.api.domain.model.activity.TrainingType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class ActivityItemResponse {

    private UUID id;
    private ActivityType type;
    private LocalDate date;
    private Double distanceKm;
    private String avgPace;
    private Integer durationSeconds;
    private TrainingType trainingType;
    private PerceivedEffort perceivedEffort;
    private String notes;
}

