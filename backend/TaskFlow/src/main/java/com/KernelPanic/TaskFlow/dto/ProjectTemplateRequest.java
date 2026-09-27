package com.KernelPanic.TaskFlow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectTemplateRequest(
        @NotBlank @Size(max = 80) String name,
        @NotBlank @Size(max = 120) String projectName,
        @Size(max = 1000) String description,
        @Size(max = 10000) String starterTasks
) {}
