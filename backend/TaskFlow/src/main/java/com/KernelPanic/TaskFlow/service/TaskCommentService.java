package com.KernelPanic.TaskFlow.service;

import com.KernelPanic.TaskFlow.dto.CreateTaskCommentRequest;
import com.KernelPanic.TaskFlow.dto.TaskCommentResponse;
import com.KernelPanic.TaskFlow.entity.TaskComment;
import com.KernelPanic.TaskFlow.entity.Task;
import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.enums.Role;
import com.KernelPanic.TaskFlow.repository.TaskCommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service @RequiredArgsConstructor
public class TaskCommentService {
    private final TaskCommentRepository comments;
    private final TaskService taskService;
    private final ChangeHistoryService historyService;

    @Transactional(readOnly = true)
    public List<TaskCommentResponse> list(Long taskId, User user) {
        Task task = taskService.findEntity(taskId);
        taskService.ensureCanAccessTask(task, user);
        return comments.findByTaskIdOrderByCreatedAtAsc(taskId).stream().map(TaskCommentResponse::fromEntity).toList();
    }

    @Transactional
    public TaskCommentResponse create(Long taskId, CreateTaskCommentRequest request, User user) {
        Task task = taskService.findEntity(taskId);
        taskService.ensureCanAccessTask(task, user);
        TaskComment comment = TaskComment.builder().task(task).author(user).content(request.content().trim()).build();
        TaskComment saved = comments.save(comment);
        historyService.record(task.getProject(), task, user, "COMMENT", saved.getId(), "CREATED",
            "comentou na tarefa \"" + task.getTitle() + "\"", java.util.Map.of("content", saved.getContent()));
        return TaskCommentResponse.fromEntity(saved);
    }

    @Transactional
    public void delete(Long taskId, Long commentId, User user) {
        Task task = taskService.findEntity(taskId);
        taskService.ensureCanAccessTask(task, user);
        TaskComment comment = comments.findById(commentId)
                .filter(row -> row.getTask().getId().equals(taskId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comentário não encontrado."));
        boolean moderator = user.getRole() == Role.MASTER || user.getRole() == Role.ADMIN
                || (user.getRole() == Role.PO && task.getProject().getOwner().getId().equals(user.getId()));
        if (!moderator && !comment.getAuthor().getId().equals(user.getId())) {
            throw new AccessDeniedException("Você só pode excluir seus próprios comentários.");
        }
        historyService.record(task.getProject(), task, user, "COMMENT", comment.getId(), "DELETED",
            "removeu um comentário da tarefa \"" + task.getTitle() + "\"", java.util.Map.of("authorId", comment.getAuthor().getId()));
        comments.delete(comment);
    }
}