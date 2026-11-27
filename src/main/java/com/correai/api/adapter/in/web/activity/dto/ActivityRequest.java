package com.correai.api.adapter.in.web.activity.dto;

import com.correai.api.domain.model.activity.ActivityType;
import com.correai.api.domain.model.activity.PerceivedEffort;
import com.correai.api.domain.model.activity.TrainingType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

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
    private String startedAt;

    public OffsetDateTime parsedStartedAt() {
        if (startedAt == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(startedAt);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(startedAt)
                        .atZone(ZoneId.of("America/Sao_Paulo"))
                        .toOffsetDateTime();
            } catch (DateTimeParseException exception) {
                throw new IllegalArgumentException(
                        "startedAt must be an ISO 8601 date-time with an optional timezone offset", exception);
            }
        }
    }
}
