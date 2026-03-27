package com.correai.api.controller;

import com.correai.api.dto.ActivityItemResponse;
import com.correai.api.dto.ActivityRequest;
import com.correai.api.dto.ActivityResponse;
import com.correai.api.service.ActivityService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/activities")
public class ActivityController {

    private final ActivityService service;

    public ActivityController(ActivityService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<ActivityItemResponse>> list(@RequestAttribute("userId") UUID userId) {
        return ResponseEntity.ok(service.list(userId));
    }

    @PostMapping
    public ResponseEntity<ActivityResponse> create(
            @RequestAttribute("userId") UUID userId,
            @Valid @RequestBody ActivityRequest request
    ) {
        return ResponseEntity.ok(service.create(userId, request));
    }
}
