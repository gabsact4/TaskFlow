package com.KernelPanic.TaskFlow.controller;

import com.KernelPanic.TaskFlow.dto.CreateTaskCommentRequest;
import com.KernelPanic.TaskFlow.dto.TaskCommentResponse;
import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.service.TaskCommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/tasks/{taskId}/comments") @RequiredArgsConstructor
public class TaskCommentController {
    private final TaskCommentService service;
    @GetMapping public List<TaskCommentResponse> list(@PathVariable Long taskId, @AuthenticationPrincipal User user) { return service.list(taskId, user); }
    @PostMapping public ResponseEntity<TaskCommentResponse> create(@PathVariable Long taskId, @Valid @RequestBody CreateTaskCommentRequest request, @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(taskId, request, user));
    }
    @DeleteMapping("/{commentId}") public ResponseEntity<Void> delete(@PathVariable Long taskId, @PathVariable Long commentId, @AuthenticationPrincipal User user) {
        service.delete(taskId, commentId, user);
        return ResponseEntity.noContent().build();
    }
}