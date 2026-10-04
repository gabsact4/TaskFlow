package com.KernelPanic.TaskFlow.dto;

import com.KernelPanic.TaskFlow.entity.TaskWorkLog;
import java.time.Instant;

public record TaskWorkLogResponse(Long id, Integer durationMinutes, String comment,
                                  Long userId, String userName, Instant createdAt) {
    public static TaskWorkLogResponse fromEntity(TaskWorkLog log) {
        return new TaskWorkLogResponse(log.getId(), log.getDurationMinutes(), log.getComment(),
                log.getUser().getId(), log.getUser().getName(), log.getCreatedAt());
    }
}
