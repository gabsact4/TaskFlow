package com.KernelPanic.TaskFlow.dto;
import java.math.BigDecimal;
import java.time.LocalDate;
public record ProjectForecastResponse(Long projectId, String projectName, BigDecimal remainingEstimatedHours, BigDecimal dailyCapacityHours, LocalDate forecastCompletionDate, int openTasks, int tasksWithoutEstimate, String confidence) {}