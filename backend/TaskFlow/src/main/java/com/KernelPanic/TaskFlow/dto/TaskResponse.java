package com.KernelPanic.TaskFlow.dto;

import com.KernelPanic.TaskFlow.entity.Task;

import java.time.Instant;
import java.time.LocalDate;

public record TaskResponse(
        Long id,
        String title,
        String description,
        String status,
        String priority,
        LocalDate dueDate,
        Long projectId,
        String projectName,
        Long assigneeId,
        String assigneeName,
        Long creatorId,
        String creatorName,
        Instant createdAt,
        Instant updatedAt
) {
    public static TaskResponse fromEntity(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus().name(),
                task.getPriority().name(),
                task.getDueDate(),
                task.getProject().getId(),
                task.getProject().getName(),
                task.getAssignee() == null ? null : task.getAssignee().getId(),
                task.getAssignee() == null ? null : task.getAssignee().getName(),
                task.getCreator().getId(),
                task.getCreator().getName(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
