package com.KernelPanic.TaskFlow.dto;
import jakarta.validation.constraints.NotNull;
public record FinishFocusSessionRequest(@NotNull Boolean completed) {}