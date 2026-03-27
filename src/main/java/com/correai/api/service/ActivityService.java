package com.correai.api.service;

import com.correai.api.domain.activity.Activity;
import com.correai.api.dto.ActivityItemResponse;
import com.correai.api.dto.ActivityRequest;
import com.correai.api.dto.ActivityResponse;
import com.correai.api.repository.ActivityRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class ActivityService {

    private final ActivityRepository repository;

    public ActivityService(ActivityRepository repository) {
        this.repository = repository;
    }

    public ActivityResponse create(UUID userId, ActivityRequest request) {

        Activity activity = Activity.create(
                userId,
                request.getType(),
                request.getDistanceKm(),
                request.getDurationSeconds(),
                request.getTrainingType(),
                request.getPerceivedEffort(),
                request.getNotes()
        );

        repository.save(activity);

        return buildResponse(userId, activity);
    }

    public List<ActivityItemResponse> list(UUID userId) {
        return repository.findByUserIdOrderByActivityDateDesc(userId)
                .stream()
                .map(this::toItemResponse)
                .toList();
    }

    private ActivityItemResponse toItemResponse(Activity activity) {
        ActivityItemResponse r = new ActivityItemResponse();
        r.setId(activity.getId());
        r.setType(activity.getType());
        r.setDate(activity.getActivityDate());
        r.setDistanceKm(activity.getDistanceKm());
        r.setDurationSeconds(activity.getDurationSeconds());
        r.setAvgPace(formatPace(activity.getAvgPaceSeconds()));
        return r;
    }

    private ActivityResponse buildResponse(UUID userId, Activity activity) {

        ActivityResponse response = new ActivityResponse();
        response.setId(activity.getId());
        response.setAvgPace(formatPace(activity.getAvgPaceSeconds()));
        response.setTotalKmMonth(totalKmCurrentMonth(userId));
        response.setStreak(calculateStreak(userId));
        response.setNewBadges(List.of()); // MVP: vazio

        return response;
    }

    private Double totalKmCurrentMonth(UUID userId) {
        LocalDate start = LocalDate.now().withDayOfMonth(1);
        LocalDate end = LocalDate.now();
        return repository.findByUserIdAndActivityDateBetween(userId, start, end)
                .stream()
                .mapToDouble(Activity::getDistanceKm)
                .sum();
    }

    private Integer calculateStreak(UUID userId) {
        int streak = 0;
        LocalDate date = LocalDate.now();

        while (repository.existsByUserIdAndActivityDate(userId, date)) {
            streak++;
            date = date.minusDays(1);
        }
        return streak;
    }

    private String formatPace(int paceSeconds) {
        int min = paceSeconds / 60;
        int sec = paceSeconds % 60;
        return String.format("%02d:%02d", min, sec);
    }
}
