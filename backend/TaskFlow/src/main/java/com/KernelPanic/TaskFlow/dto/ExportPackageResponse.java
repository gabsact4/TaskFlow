package com.KernelPanic.TaskFlow.dto;
import java.time.Instant;
import java.util.List;
public record ExportPackageResponse(Instant generatedAt, List<ProjectResponse> projects, List<TaskResponse> tasks, List<ExportWorkLog> workLogs, List<TaskCommentResponse> comments, List<ChangeHistoryResponse> history) {
    public record ExportWorkLog(Long projectId, Long taskId, String taskTitle, TaskWorkLogResponse workLog) {}
}