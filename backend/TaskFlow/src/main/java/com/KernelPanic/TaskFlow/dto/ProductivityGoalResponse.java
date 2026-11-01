package com.KernelPanic.TaskFlow.dto;
import com.KernelPanic.TaskFlow.entity.ProductivityGoal;
import java.math.BigDecimal;
import java.time.LocalDate;
public record ProductivityGoalResponse(Long id, String metric, String periodType, BigDecimal targetValue, BigDecimal currentValue, double progressPercent, LocalDate periodStart, LocalDate periodEnd) {
    public static ProductivityGoalResponse fromEntity(ProductivityGoal goal, BigDecimal current, LocalDate start, LocalDate end) {
        double progress = goal.getTargetValue().signum() == 0 ? 0 : current.multiply(BigDecimal.valueOf(100)).divide(goal.getTargetValue(), 2, java.math.RoundingMode.HALF_UP).doubleValue();
        return new ProductivityGoalResponse(goal.getId(), goal.getMetric().name(), goal.getPeriodType().name(), goal.getTargetValue(), current, progress, start, end);
    }
}