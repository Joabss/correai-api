package com.correai.api.domain.model.goal;

public enum GoalType {
    /** Total distance in km. */
    DISTANCE_KM,
    /** Number of activities. */
    ACTIVITIES,
    /** Average pace in seconds per km; reached when the pace is at or below the target. */
    AVG_PACE
}
