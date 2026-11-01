package com.KernelPanic.TaskFlow.dto;
import com.KernelPanic.TaskFlow.entity.TaskTimerSession;
import java.time.Instant;
public record TaskTimerResponse(Long id, Long taskId, String taskTitle, String status, Instant startedAt, Instant pausedAt, long elapsedSeconds) {
    public static TaskTimerResponse fromEntity(TaskTimerSession session, long elapsedSeconds) {
        return new TaskTimerResponse(session.getId(), session.getTask().getId(), session.getTask().getTitle(), session.getStatus().name(), session.getStartedAt(), session.getPausedAt(), elapsedSeconds);
    }
}