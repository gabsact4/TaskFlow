package com.KernelPanic.TaskFlow.service;

import com.KernelPanic.TaskFlow.dto.*;
import com.KernelPanic.TaskFlow.entity.Project;
import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.repository.TaskCommentRepository;
import com.KernelPanic.TaskFlow.repository.TaskWorkLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Service @RequiredArgsConstructor
public class ExportService {
    private final ProjectService projectService;
    private final TaskService taskService;
    private final TaskWorkLogRepository workLogs;
    private final TaskCommentRepository comments;
    private final ChangeHistoryService historyService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public ExportPackageResponse data(User user, Long projectId) {
        List<ProjectResponse> visible = projectId == null ? projectService.list(user) : List.of(projectService.get(projectId, user));
        List<Project> projectEntities = visible.stream().map(project -> projectService.findEntity(project.id())).toList();
        List<Long> ids = projectEntities.stream().map(project -> project.getId()).toList();
        List<TaskResponse> taskRows = ids.stream().flatMap(id -> taskService.list(id, user).stream()).toList();
        Instant exportTime = Instant.now();
        List<ExportPackageResponse.ExportWorkLog> logRows = taskRows.stream().flatMap(task -> workLogs.findByTaskIdOrderByCreatedAtDesc(task.id()).stream()
            .map(log -> new ExportPackageResponse.ExportWorkLog(task.projectId(), task.id(), task.title(), TaskWorkLogResponse.fromEntity(log)))).toList();
        List<TaskCommentResponse> commentRows = taskRows.stream().flatMap(task -> comments.findByTaskIdOrderByCreatedAtAsc(task.id()).stream())
                .map(TaskCommentResponse::fromEntity).toList();
        boolean globalMasterExport = projectId == null && (user.getRole() == com.KernelPanic.TaskFlow.enums.Role.MASTER || user.getRole() == com.KernelPanic.TaskFlow.enums.Role.ADMIN);
        List<ChangeHistoryResponse> history = historyService.exportForProjects(ids, taskRows.stream().map(TaskResponse::id).toList(), user, globalMasterExport).stream().map(ChangeHistoryResponse::fromEntity).toList();
        return new ExportPackageResponse(exportTime, visible, taskRows, logRows, commentRows, history);
    }

    @Transactional(readOnly = true)
    public byte[] json(User user, Long projectId) {
        try { return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(data(user, projectId)); }
        catch (JsonProcessingException e) { throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possível gerar o arquivo JSON.", e); }
    }

    @Transactional(readOnly = true)
    public byte[] csv(User user, Long projectId) {
        ExportPackageResponse export = data(user, projectId);
        StringBuilder csv = new StringBuilder("\uFEFFrecord_type,id,project_id,project_name,task_id,task_title,user_id,user_name,timestamp,data\r\n");
        export.projects().forEach(project -> row(csv, "project", project.id(), project.id(), project.name(), null, null, project.ownerId(), project.ownerName(), project.updatedAt(), project));
        export.tasks().forEach(task -> row(csv, "task", task.id(), task.projectId(), task.projectName(), task.id(), task.title(), task.assigneeId(), task.assigneeName(), task.updatedAt(), task));
        export.workLogs().forEach(entry -> row(csv, "work_log", entry.workLog().id(), entry.projectId(), projectName(export.projects(), entry.projectId()), entry.taskId(), entry.taskTitle(), entry.workLog().userId(), entry.workLog().userName(), entry.workLog().createdAt(), entry.workLog()));
        export.comments().forEach(comment -> row(csv, "comment", comment.id(), projectForTask(export.tasks(), comment.taskId()), projectNameForTask(export.tasks(), comment.taskId()), comment.taskId(), taskName(export.tasks(), comment.taskId()), comment.authorId(), comment.authorName(), comment.createdAt(), comment));
        export.history().forEach(item -> row(csv, "history", item.id(), item.projectId(), projectName(export.projects(), item.projectId()), item.taskId(), taskName(export.tasks(), item.taskId()), item.actorId(), item.actorName(), item.createdAt(), item));
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void row(StringBuilder output, Object... values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) output.append(',');
            Object value = values[i];
            String text;
            if (value == null) text = "";
            else if (i == values.length - 1) {
                try { text = objectMapper.writeValueAsString(value); }
                catch (JsonProcessingException e) { text = "{}"; }
            } else text = String.valueOf(value);
            if (!text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0) text = "'" + text;
            output.append('"').append(text.replace("\"", "\"\"")).append('"');
        }
        output.append("\r\n");
    }
    private String projectName(List<ProjectResponse> projects, Long id) { return projects.stream().filter(project -> project.id().equals(id)).map(ProjectResponse::name).findFirst().orElse(""); }
    private Long projectForTask(List<TaskResponse> tasks, Long id) { return tasks.stream().filter(task -> task.id().equals(id)).map(TaskResponse::projectId).findFirst().orElse(null); }
    private String projectNameForTask(List<TaskResponse> tasks, Long id) { return tasks.stream().filter(task -> task.id().equals(id)).map(TaskResponse::projectName).findFirst().orElse(""); }
    private String taskName(List<TaskResponse> tasks, Long id) { return id == null ? "" : tasks.stream().filter(task -> task.id().equals(id)).map(TaskResponse::title).findFirst().orElse(""); }
}