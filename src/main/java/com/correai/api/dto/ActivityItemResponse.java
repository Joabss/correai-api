package com.correai.api.dto;

import com.correai.api.domain.activity.ActivityType;
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
}

