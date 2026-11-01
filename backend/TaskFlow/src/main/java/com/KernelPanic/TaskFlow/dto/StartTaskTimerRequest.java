package com.KernelPanic.TaskFlow.dto;
import jakarta.validation.constraints.NotNull;
public record StartTaskTimerRequest(@NotNull Long taskId) {}