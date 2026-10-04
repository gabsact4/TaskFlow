package com.KernelPanic.TaskFlow.dto;

import com.KernelPanic.TaskFlow.enums.TaskPriority;
import com.KernelPanic.TaskFlow.enums.TaskRecurrence;
import com.KernelPanic.TaskFlow.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

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
        @NotNull Long version
) {}
