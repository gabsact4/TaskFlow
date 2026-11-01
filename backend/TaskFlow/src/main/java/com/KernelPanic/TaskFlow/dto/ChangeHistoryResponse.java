package com.KernelPanic.TaskFlow.dto;
import com.KernelPanic.TaskFlow.entity.ChangeHistory;
import java.time.Instant;
public record ChangeHistoryResponse(Long id, Long projectId, Long actorId, String actorName, String entityType, Long entityId, Long taskId, String action, String summary, String detailsJson, Instant createdAt) {
    public static ChangeHistoryResponse fromEntity(ChangeHistory item) {
        return new ChangeHistoryResponse(item.getId(), item.getProjectId(), item.getActorId(), item.getActorName(), item.getEntityType(), item.getEntityId(), item.getTaskId(), item.getAction(), item.getSummary(), item.getDetailsJson(), item.getCreatedAt());
    }
}