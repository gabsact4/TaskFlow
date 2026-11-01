package com.KernelPanic.TaskFlow.dto;
import com.KernelPanic.TaskFlow.entity.TaskAttachment;
import java.time.Instant;
public record TaskAttachmentResponse(Long id, Long taskId, Long uploaderId, String uploaderName, String originalName, String contentType, Long sizeBytes, Instant createdAt) {
    public static TaskAttachmentResponse fromEntity(TaskAttachment a) { return new TaskAttachmentResponse(a.getId(), a.getTask().getId(), a.getUploader().getId(), a.getUploader().getName(), a.getOriginalName(), a.getContentType(), a.getSizeBytes(), a.getCreatedAt()); }
}