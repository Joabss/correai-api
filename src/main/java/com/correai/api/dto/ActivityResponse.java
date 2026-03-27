package com.correai.api.dto;

import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActivityResponse {

    private UUID id;
    private String avgPace;
    private Double totalKmMonth;
    private Integer streak;
    private List<String> newBadges;
}
