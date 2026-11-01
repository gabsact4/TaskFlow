package com.KernelPanic.TaskFlow.dto;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
public record FocusSessionRequest(Long taskId, @Min(1) @Max(120) Integer plannedMinutes) {}