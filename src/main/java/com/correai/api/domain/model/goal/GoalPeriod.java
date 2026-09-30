package com.correai.api.domain.model.goal;

import java.time.DayOfWeek;
import java.time.LocalDate;

public enum GoalPeriod {
    WEEKLY,
    MONTHLY;

    public LocalDate startOf(LocalDate today) {
        return this == WEEKLY ? today.with(DayOfWeek.MONDAY) : today.withDayOfMonth(1);
    }

    public LocalDate endOf(LocalDate today) {
        return this == WEEKLY ? startOf(today).plusDays(6) : today.withDayOfMonth(today.lengthOfMonth());
    }
}
