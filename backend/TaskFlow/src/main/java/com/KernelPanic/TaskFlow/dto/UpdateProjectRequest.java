package com.KernelPanic.TaskFlow.dto;

import com.KernelPanic.TaskFlow.enums.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateProjectRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 1000) String description,
        ProjectStatus status,
        List<Long> memberIds
) {
    public UpdateProjectRequest(String name, String description, ProjectStatus status) { this(name, description, status, null); }
}
