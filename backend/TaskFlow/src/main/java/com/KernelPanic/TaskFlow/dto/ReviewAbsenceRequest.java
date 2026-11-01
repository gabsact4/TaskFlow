package com.KernelPanic.TaskFlow.dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
public record ReviewAbsenceRequest(@NotNull Boolean approved, @Size(max = 500) String note) {}