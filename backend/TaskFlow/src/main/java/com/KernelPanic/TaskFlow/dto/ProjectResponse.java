package com.KernelPanic.TaskFlow.dto;

import com.KernelPanic.TaskFlow.entity.Project;

import java.time.Instant;

public record ProjectResponse(
        Long id,
        String name,
        String projectKey,
        String description,
        String status,
        Long ownerId,
        String ownerName,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProjectResponse fromEntity(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getProjectKey(),
                project.getDescription(),
                project.getStatus().name(),
                project.getOwner().getId(),
                project.getOwner().getName(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
