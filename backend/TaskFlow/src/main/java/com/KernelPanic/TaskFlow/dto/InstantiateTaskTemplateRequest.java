package com.KernelPanic.TaskFlow.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record InstantiateTaskTemplateRequest(@NotNull Long projectId, Long assigneeId, LocalDate dueDate, Long parentTaskId) {}
