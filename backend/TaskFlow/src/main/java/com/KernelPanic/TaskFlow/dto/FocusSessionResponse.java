package com.KernelPanic.TaskFlow.dto;
import com.KernelPanic.TaskFlow.entity.FocusSession;
import java.time.Instant;
public record FocusSessionResponse(Long id, Long taskId, String taskTitle, Instant startedAt, Instant endedAt, Integer plannedMinutes, Integer actualMinutes, Boolean completed) {
    public static FocusSessionResponse fromEntity(FocusSession session) { return new FocusSessionResponse(session.getId(), session.getTask() == null ? null : session.getTask().getId(), session.getTask() == null ? null : session.getTask().getTitle(), session.getStartedAt(), session.getEndedAt(), session.getPlannedMinutes(), session.getActualMinutes(), session.getCompleted()); }
}