package com.correai.api.adapter.in.web.goal;

import com.correai.api.adapter.in.web.activity.PaceFormatter;
import com.correai.api.domain.model.goal.Goal;
import com.correai.api.domain.model.goal.GoalPeriod;
import com.correai.api.domain.model.goal.GoalType;
import com.correai.api.domain.port.in.goal.GoalProgress;

import java.time.LocalDate;
import java.util.UUID;

/**
 * For {@code AVG_PACE} goals, {@code target} and {@code current} are seconds per km and the
 * {@code targetPace}/{@code currentPace} fields carry the same values formatted as mm:ss.
 */
public record GoalResponse(
        UUID id,
        GoalType type,
        GoalPeriod period,
        double target,
        String targetPace,
        LocalDate periodStart,
        LocalDate periodEnd,
        Double current,
        String currentPace,
        Double percentage,
        boolean achieved
) {

    public static GoalResponse from(Goal goal) {
        return new GoalResponse(goal.id(), goal.type(), goal.period(), goal.target(), paceOf(goal, goal.target()),
                null, null, null, null, null, false);
    }

    public static GoalResponse from(GoalProgress progress) {
        Goal goal = progress.goal();
        return new GoalResponse(goal.id(), goal.type(), goal.period(), goal.target(), paceOf(goal, goal.target()),
                progress.periodStart(), progress.periodEnd(), progress.current(),
                progress.current() == null ? null : paceOf(goal, progress.current()),
                progress.percentage(), progress.achieved());
    }

    private static String paceOf(Goal goal, double seconds) {
        return goal.type() == GoalType.AVG_PACE ? PaceFormatter.format((int) seconds) : null;
    }
}
