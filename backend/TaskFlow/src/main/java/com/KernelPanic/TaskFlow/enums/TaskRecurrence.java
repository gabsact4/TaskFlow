package com.KernelPanic.TaskFlow.enums;

import java.time.LocalDate;

public enum TaskRecurrence {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY;

    public LocalDate next(LocalDate date) {
        return switch (this) {
            case DAILY -> date.plusDays(1);
            case WEEKLY -> date.plusWeeks(1);
            case MONTHLY -> date.plusMonths(1);
            case NONE -> null;
        };
    }
}
