package com.KernelPanic.TaskFlow.dto;

import com.KernelPanic.TaskFlow.enums.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 1000) String description,
        ProjectStatus status
) {}
