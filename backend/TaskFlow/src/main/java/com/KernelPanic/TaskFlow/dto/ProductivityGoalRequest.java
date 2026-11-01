package com.KernelPanic.TaskFlow.dto;
import com.KernelPanic.TaskFlow.entity.ProductivityGoal;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
public record ProductivityGoalRequest(@NotNull ProductivityGoal.Metric metric, @NotNull ProductivityGoal.PeriodType periodType, @NotNull @DecimalMin("0.25") @DecimalMax("100000.00") BigDecimal targetValue) {}