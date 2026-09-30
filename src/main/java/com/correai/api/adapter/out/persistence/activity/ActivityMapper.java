package com.correai.api.adapter.out.persistence.activity;

import com.correai.api.domain.model.activity.Activity;
import org.springframework.stereotype.Component;

@Component
public class ActivityMapper {

    public ActivityEntity toEntity(Activity activity) {
        return new ActivityEntity(
                activity.id(),
                activity.userId(),
                activity.type(),
                activity.activityDate(),
                activity.distanceKm(),
                activity.durationSeconds(),
                activity.avgPaceSeconds(),
                activity.trainingType(),
                activity.perceivedEffort(),
                activity.notes(),
                activity.createdAt()
        );
    }

    public Activity toDomain(ActivityEntity entity) {
        return Activity.reconstruct(
                entity.getId(),
                entity.getUserId(),
                entity.getType(),
                entity.getActivityDate(),
                entity.getDistanceKm(),
                entity.getDurationSeconds(),
                entity.getAvgPaceSeconds(),
                entity.getTrainingType(),
                entity.getPerceivedEffort(),
                entity.getNotes(),
                entity.getCreatedAt()
        );
    }
}

