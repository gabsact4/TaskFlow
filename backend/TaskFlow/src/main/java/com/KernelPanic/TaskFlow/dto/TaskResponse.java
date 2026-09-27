package com.KernelPanic.TaskFlow.dto;

import com.KernelPanic.TaskFlow.entity.Task;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TaskResponse(
        Long id,
        String title,
        String description,
        String status,
        String priority,
        String recurrence,
        LocalDate recurrenceEndDate,
        LocalDate nextOccurrenceDate,
        LocalDate dueDate,
        Long projectId,
        String projectName,
        Long assigneeId,
        String assigneeName,
        Long creatorId,
        String creatorName,
        Long parentTaskId,
        List<TaskChecklistItemResponse> checklist,
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
                task.getRecurrence().name(),
                task.getRecurrenceEndDate(),
                task.getNextOccurrenceDate(),
                task.getDueDate(),
                task.getProject().getId(),
                task.getProject().getName(),
                task.getAssignee() == null ? null : task.getAssignee().getId(),
                task.getAssignee() == null ? null : task.getAssignee().getName(),
                task.getCreator().getId(),
                task.getCreator().getName(),
                task.getParentTask() == null ? null : task.getParentTask().getId(),
                task.getChecklistItems().stream().map(TaskChecklistItemResponse::fromEntity).toList(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
