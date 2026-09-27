package com.KernelPanic.TaskFlow.dto;

import com.KernelPanic.TaskFlow.entity.ProjectTemplate;

public record ProjectTemplateResponse(Long id, String name, String projectName, String description, String starterTasks) {
    public static ProjectTemplateResponse fromEntity(ProjectTemplate template) {
        return new ProjectTemplateResponse(template.getId(), template.getName(), template.getProjectName(),
                template.getDescription(), template.getStarterTasks());
    }
}
