package com.correai.api.adapter.in.web.stats.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatsSummaryResponse {

    private Double kmWeek;
    private Double kmMonth;
    private Integer activitiesWeek;
    private Integer streak;
    private Double longestDistance;
}

