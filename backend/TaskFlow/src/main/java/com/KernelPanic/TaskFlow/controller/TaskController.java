package com.KernelPanic.TaskFlow.controller;

import com.KernelPanic.TaskFlow.dto.CreateTaskRequest;
import com.KernelPanic.TaskFlow.dto.CreateChecklistItemRequest;
import com.KernelPanic.TaskFlow.dto.UpdateChecklistItemRequest;
import com.KernelPanic.TaskFlow.dto.TaskChecklistItemResponse;
import com.KernelPanic.TaskFlow.dto.TaskResponse;
import com.KernelPanic.TaskFlow.dto.UpdateTaskRequest;
import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.enums.TaskStatus;
import com.KernelPanic.TaskFlow.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<TaskResponse> create(
            @Valid @RequestBody CreateTaskRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskService.create(request, currentUser));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.get(id));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<TaskResponse>> listByProject(
            @PathVariable Long projectId,
            @RequestParam(required = false) TaskStatus status) {
        if (status == null) {
            return ResponseEntity.ok(taskService.list(projectId));
        }
        return ResponseEntity.ok(taskService.listByStatus(projectId, status));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(taskService.update(id, request, currentUser));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        taskService.delete(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{taskId}/checklist")
    public ResponseEntity<List<TaskChecklistItemResponse>> listChecklist(
            @PathVariable Long taskId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(taskService.listChecklist(taskId, currentUser));
    }

    @PostMapping("/{taskId}/checklist")
    public ResponseEntity<TaskChecklistItemResponse> addChecklistItem(
            @PathVariable Long taskId,
            @Valid @RequestBody CreateChecklistItemRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.addChecklistItem(taskId, request, currentUser));
    }

    @PatchMapping("/{taskId}/checklist/{itemId}")
    public ResponseEntity<TaskChecklistItemResponse> updateChecklistItem(
            @PathVariable Long taskId,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateChecklistItemRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(taskService.updateChecklistItem(taskId, itemId, request, currentUser));
    }

    @DeleteMapping("/{taskId}/checklist/{itemId}")
    public ResponseEntity<Void> deleteChecklistItem(
            @PathVariable Long taskId,
            @PathVariable Long itemId,
            @AuthenticationPrincipal User currentUser) {
        taskService.deleteChecklistItem(taskId, itemId, currentUser);
        return ResponseEntity.noContent().build();
    }
}
