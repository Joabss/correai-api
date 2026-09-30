package com.correai.api.adapter.in.web.stats.dto;

import java.time.LocalDate;
import java.util.List;

public record StatsEvolutionResponse(int weeks, List<Week> items) {

    public record Week(LocalDate weekStart, double km, int activities, String avgPace) {
    }
}
