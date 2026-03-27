package com.correai.api.domain.activity;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import lombok.Setter;

@Entity
@Table(name = "activities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Activity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    private ActivityType type; // RUN | WALK

    private LocalDate activityDate;

    private Double distanceKm;

    private Integer durationSeconds;

    @Setter(AccessLevel.PRIVATE)
    private Integer avgPaceSeconds;

    @Enumerated(EnumType.STRING)
    private TrainingType trainingType;

    @Enumerated(EnumType.STRING)
    private PerceivedEffort perceivedEffort;

    private String notes;

    private Instant createdAt;

    private Activity(UUID userId, ActivityType type, LocalDate activityDate, double distanceKm, int durationSeconds, TrainingType trainingType, PerceivedEffort perceivedEffort, String notes) {
        this.userId = Objects.requireNonNull(userId);
        this.type = Objects.requireNonNull(type);
        this.activityDate = activityDate != null ? activityDate : LocalDate.now();
        this.distanceKm = distanceKm;
        this.durationSeconds = durationSeconds;
        this.trainingType = trainingType;
        this.perceivedEffort = perceivedEffort;
        this.notes = notes;
        this.avgPaceSeconds = calculatePace();
        this.createdAt = Instant.now();
    }

    public static Activity create(UUID userId, ActivityType type, double distanceKm, int durationSeconds, TrainingType trainingType, PerceivedEffort perceivedEffort, String notes) {
        validate(distanceKm, durationSeconds);

        return new Activity(userId, type, LocalDate.now(), distanceKm, durationSeconds, trainingType, perceivedEffort, notes);
    }

    private static void validate(double distanceKm, int durationSeconds) {
        if (distanceKm <= 0) {
            throw new IllegalArgumentException("Distance must be greater than zero");
        }
        if (durationSeconds <= 0) {
            throw new IllegalArgumentException("Duration must be greater than zero");
        }
    }

    private int calculatePace() {
        return (int) (durationSeconds / distanceKm);
    }

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
    }

}
