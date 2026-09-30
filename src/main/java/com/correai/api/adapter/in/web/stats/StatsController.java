package com.correai.api.adapter.in.web.stats;

import com.correai.api.domain.port.in.stats.GetStatsSummaryUseCase;
import com.correai.api.domain.port.in.stats.StatsSummary;
import com.correai.api.adapter.in.web.stats.dto.StatsSummaryResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/stats")
public class StatsController {

    private final GetStatsSummaryUseCase getStatsSummaryUseCase;

    public StatsController(GetStatsSummaryUseCase getStatsSummaryUseCase) {
        this.getStatsSummaryUseCase = getStatsSummaryUseCase;
    }

    @GetMapping("/summary")
    public ResponseEntity<StatsSummaryResponse> getSummary(
            @RequestAttribute("userId") UUID userId
    ) {
        StatsSummary summary = getStatsSummaryUseCase.getSummary(userId);

        StatsSummaryResponse response = new StatsSummaryResponse();
        response.setKmWeek(summary.kmWeek());
        response.setKmMonth(summary.kmMonth());
        response.setActivitiesWeek(summary.activitiesWeek());
        response.setStreak(summary.streak());
        response.setLongestDistance(summary.longestDistance());

        return ResponseEntity.ok(response);
    }
}

