package com.correai.api.adapter.in.web.stats;

import com.correai.api.domain.port.in.stats.GetStatsEvolutionUseCase;
import com.correai.api.domain.port.in.stats.GetStatsSummaryUseCase;
import com.correai.api.domain.port.in.stats.StatsSummary;
import com.correai.api.adapter.in.web.activity.PaceFormatter;
import com.correai.api.adapter.in.web.stats.dto.StatsEvolutionResponse;
import com.correai.api.adapter.in.web.stats.dto.StatsSummaryResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/stats")
public class StatsController {

    private final GetStatsSummaryUseCase getStatsSummaryUseCase;
    private final GetStatsEvolutionUseCase getStatsEvolutionUseCase;

    public StatsController(GetStatsSummaryUseCase getStatsSummaryUseCase,
                           GetStatsEvolutionUseCase getStatsEvolutionUseCase) {
        this.getStatsSummaryUseCase = getStatsSummaryUseCase;
        this.getStatsEvolutionUseCase = getStatsEvolutionUseCase;
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

    @GetMapping("/evolution")
    public ResponseEntity<StatsEvolutionResponse> getEvolution(
            @RequestAttribute("userId") UUID userId,
            @RequestParam(defaultValue = "8") int weeks
    ) {
        List<StatsEvolutionResponse.Week> items = getStatsEvolutionUseCase.getEvolution(userId, weeks)
                .stream()
                .map(week -> new StatsEvolutionResponse.Week(
                        week.weekStart(), week.km(), week.activities(), PaceFormatter.format(week.avgPaceSeconds())))
                .toList();
        return ResponseEntity.ok(new StatsEvolutionResponse(weeks, items));
    }
}
