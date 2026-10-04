package com.KernelPanic.TaskFlow.controller;

import com.KernelPanic.TaskFlow.dto.CreateProjectRequest;
import com.KernelPanic.TaskFlow.dto.ProjectResponse;
import com.KernelPanic.TaskFlow.dto.UpdateProjectRequest;
import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<ProjectResponse> create(
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.create(request, currentUser));
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> list(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(projectService.list(currentUser));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<ProjectResponse>> listMine(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(projectService.listMine(currentUser));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> get(@PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(projectService.get(id, currentUser));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProjectRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(projectService.update(id, request, currentUser));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        projectService.delete(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
