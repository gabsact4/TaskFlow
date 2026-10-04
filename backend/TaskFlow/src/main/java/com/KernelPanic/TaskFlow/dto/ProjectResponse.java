package com.KernelPanic.TaskFlow.dto;

import com.KernelPanic.TaskFlow.entity.Project;

import java.time.Instant;
import java.util.List;

public record ProjectResponse(
        Long id,
        String name,
        String projectKey,
        String description,
        String status,
        Long ownerId,
        String ownerName,
        Instant createdAt,
        Instant updatedAt,
        List<Long> memberIds
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
                project.getUpdatedAt(),
                java.util.stream.Stream.concat(java.util.stream.Stream.of(project.getOwner().getId()), project.getMembers().stream().map(com.KernelPanic.TaskFlow.entity.User::getId)).distinct().toList()
        );
    }
}
