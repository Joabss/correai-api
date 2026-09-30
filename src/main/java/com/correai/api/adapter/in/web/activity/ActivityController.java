package com.correai.api.adapter.in.web.activity;

import com.correai.api.adapter.in.web.dto.PageResponse;
import com.correai.api.domain.model.activity.Activity;
import com.correai.api.domain.model.pagination.PageQuery;
import com.correai.api.domain.model.pagination.PageResult;
import com.correai.api.domain.port.in.activity.ActivityCreationResult;
import com.correai.api.domain.port.in.activity.CreateActivityCommand;
import com.correai.api.domain.port.in.activity.CreateActivityUseCase;
import com.correai.api.domain.port.in.activity.ListActivitiesUseCase;
import com.correai.api.adapter.in.web.activity.dto.ActivityItemResponse;
import com.correai.api.adapter.in.web.activity.dto.ActivityRequest;
import com.correai.api.adapter.in.web.activity.dto.ActivityResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/activities")
public class ActivityController {

    private final CreateActivityUseCase createActivityUseCase;
    private final ListActivitiesUseCase listActivitiesUseCase;

    public ActivityController(CreateActivityUseCase createActivityUseCase, ListActivitiesUseCase listActivitiesUseCase) {
        this.createActivityUseCase = createActivityUseCase;
        this.listActivitiesUseCase = listActivitiesUseCase;
    }

    @GetMapping
    public ResponseEntity<PageResponse<ActivityItemResponse>> list(
            @RequestAttribute("userId") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + PageQuery.DEFAULT_SIZE) int size
    ) {
        PageQuery pageQuery;
        try {
            pageQuery = new PageQuery(page, size);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
        PageResult<ActivityItemResponse> result = listActivitiesUseCase.list(userId, pageQuery)
                .map(this::toItemResponse);
        return ResponseEntity.ok(PageResponse.from(result));
    }

    @PostMapping
    public ResponseEntity<ActivityResponse> create(
            @RequestAttribute("userId") UUID userId,
            @Valid @RequestBody ActivityRequest request
    ) {
        CreateActivityCommand command = new CreateActivityCommand(
                userId,
                request.getType(),
                request.getDistanceKm(),
                request.getDurationSeconds(),
                request.getTrainingType(),
                request.getPerceivedEffort(),
                request.getNotes()
        );

        ActivityCreationResult result = createActivityUseCase.create(command);

        return ResponseEntity.ok(toResponse(result));
    }

    private ActivityItemResponse toItemResponse(Activity activity) {
        ActivityItemResponse r = new ActivityItemResponse();
        r.setId(activity.id());
        r.setType(activity.type());
        r.setDate(activity.activityDate());
        r.setDistanceKm(activity.distanceKm());
        r.setDurationSeconds(activity.durationSeconds());
        r.setAvgPace(PaceFormatter.format(activity.avgPaceSeconds()));
        return r;
    }

    private ActivityResponse toResponse(ActivityCreationResult result) {
        ActivityResponse response = new ActivityResponse();
        response.setId(result.activity().id());
        response.setAvgPace(PaceFormatter.format(result.activity().avgPaceSeconds()));
        response.setTotalKmMonth(result.totalKmMonth());
        response.setStreak(result.streak());
        response.setNewBadges(result.newBadges());
        return response;
    }
}

