package com.KernelPanic.TaskFlow.dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
public record CreateAbsenceRequest(@NotNull LocalDate startDate, @NotNull LocalDate endDate, @Size(max = 500) String reason) {}