package com.KernelPanic.TaskFlow.dto;

import com.KernelPanic.TaskFlow.enums.TaskPriority;
import com.KernelPanic.TaskFlow.enums.TaskRecurrence;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TaskTemplateRequest(
        @NotBlank @Size(max = 80) String name,
        @NotBlank @Size(max = 180) String title,
        @Size(max = 2000) String description,
        TaskPriority priority,
        TaskRecurrence recurrence,
        LocalDate recurrenceEndDate,
        @Size(max = 5000) String checklistItems
) {}
