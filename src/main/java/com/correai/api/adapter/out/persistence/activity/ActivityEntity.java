package com.correai.api.adapter.out.persistence.activity;

import com.correai.api.domain.model.activity.ActivityType;
import com.correai.api.domain.model.activity.PerceivedEffort;
import com.correai.api.domain.model.activity.TrainingType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Entity
@Table(name = "activities")
@NoArgsConstructor
@AllArgsConstructor
public class ActivityEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    private ActivityType type;

    private LocalDate activityDate;

    private Double distanceKm;

    private Integer durationSeconds;

    private Integer avgPaceSeconds;

    @Enumerated(EnumType.STRING)
    private TrainingType trainingType;

    @Enumerated(EnumType.STRING)
    private PerceivedEffort perceivedEffort;

    private String notes;

    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

}

