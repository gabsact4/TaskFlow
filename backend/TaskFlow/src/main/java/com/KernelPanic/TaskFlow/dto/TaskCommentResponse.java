package com.KernelPanic.TaskFlow.dto;
import com.KernelPanic.TaskFlow.entity.TaskComment;
import java.time.Instant;
public record TaskCommentResponse(Long id, Long taskId, Long authorId, String authorName, String content, Instant createdAt, Instant updatedAt) {
    public static TaskCommentResponse fromEntity(TaskComment c) { return new TaskCommentResponse(c.getId(), c.getTask().getId(), c.getAuthor().getId(), c.getAuthor().getName(), c.getContent(), c.getCreatedAt(), c.getUpdatedAt()); }
}