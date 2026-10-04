package com.KernelPanic.TaskFlow.dto;

import com.KernelPanic.TaskFlow.entity.Notification;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        String type,
        String title,
        String message,
        Long taskId,
        Instant createdAt,
        Instant readAt
) {
    public static NotificationResponse fromEntity(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getTask() == null ? null : notification.getTask().getId(),
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }
}
