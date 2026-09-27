package com.KernelPanic.TaskFlow.controller;

import com.KernelPanic.TaskFlow.dto.*;
import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.service.TemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/task-templates")
@RequiredArgsConstructor
public class TaskTemplateController {
    private final TemplateService templateService;

    @GetMapping
    public ResponseEntity<List<TaskTemplateResponse>> list(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(templateService.listTaskTemplates(user));
    }

    @PostMapping
    public ResponseEntity<TaskTemplateResponse> create(@Valid @RequestBody TaskTemplateRequest request,
                                                       @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.createTaskTemplate(request, user));
    }

    @PostMapping("/{id}/tasks")
    public ResponseEntity<TaskResponse> instantiate(@PathVariable Long id,
                                                    @Valid @RequestBody InstantiateTaskTemplateRequest request,
                                                    @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.instantiateTaskTemplate(id, request, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal User user) {
        templateService.deleteTaskTemplate(id, user);
        return ResponseEntity.noContent().build();
    }
}
