package com.KernelPanic.TaskFlow.dto;

import com.KernelPanic.TaskFlow.enums.TaskPriority;
import com.KernelPanic.TaskFlow.enums.TaskRecurrence;
import com.KernelPanic.TaskFlow.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;

import java.time.LocalDate;
import java.math.BigDecimal;

public record UpdateTaskRequest(
        @NotBlank @Size(max = 180) String title,
        @Size(max = 2000) String description,
        @NotNull TaskStatus status,
        @NotNull TaskPriority priority,
        LocalDate dueDate,
        @NotNull Long projectId,
        Long assigneeId,
        TaskRecurrence recurrence,
        LocalDate recurrenceEndDate,
        @NotNull Long version,
        LocalDate plannedStartDate,
        @DecimalMin("0.25") @DecimalMax("10000.00") BigDecimal estimatedHours
) {}
