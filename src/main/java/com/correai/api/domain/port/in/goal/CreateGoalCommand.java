package com.correai.api.domain.port.in.goal;

import com.correai.api.domain.model.goal.GoalPeriod;
import com.correai.api.domain.model.goal.GoalType;

import java.util.UUID;

public record CreateGoalCommand(UUID userId, GoalType type, GoalPeriod period, double target) {
}
