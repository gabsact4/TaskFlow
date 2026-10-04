package com.KernelPanic.TaskFlow.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record CreateTaskWorkLogRequest(
        @Min(value = 1, message = "Informe pelo menos um minuto trabalhado")
        @Max(value = 1440, message = "Cada lançamento pode ter no máximo 24 horas")
        int durationMinutes,
        @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres")
        String comment
) {}
