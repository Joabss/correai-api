package com.correai.api.domain.port.in.activity;

import com.correai.api.domain.model.activity.Activity;

import java.util.List;

/**
 * Result of creating an activity: the activity itself plus derived stats used by the MVP response.
 */
public record ActivityCreationResult(
        Activity activity,
        double totalKmMonth,
        int streak,
        List<String> newBadges
) {
}

