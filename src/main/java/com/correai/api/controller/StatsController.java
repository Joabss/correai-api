package com.correai.api.controller;

import com.correai.api.dto.StatsSummaryResponse;
import com.correai.api.service.StatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/stats")
public class StatsController {

    private final StatsService service;

    public StatsController(StatsService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public ResponseEntity<StatsSummaryResponse> getSummary(
            @RequestAttribute("userId") UUID userId
    ) {
        return ResponseEntity.ok(service.getSummary(userId));
    }
}
