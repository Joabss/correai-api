package com.correai.api.domain.port.in.activity;

import com.correai.api.domain.model.activity.ActivityType;
import com.correai.api.domain.model.activity.PerceivedEffort;
import com.correai.api.domain.model.activity.TrainingType;

import java.util.UUID;

/**
 * Command to create a new Activity.
 */
public record CreateActivityCommand(
        UUID userId,
        ActivityType type,
        double distanceKm,
        int durationSeconds,
        TrainingType trainingType,
        PerceivedEffort perceivedEffort,
        String notes
) {
}

