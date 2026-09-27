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
@RequestMapping("/api/project-templates")
@RequiredArgsConstructor
public class ProjectTemplateController {
    private final TemplateService templateService;

    @GetMapping
    public ResponseEntity<List<ProjectTemplateResponse>> list(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(templateService.listProjectTemplates(user));
    }

    @PostMapping
    public ResponseEntity<ProjectTemplateResponse> create(@Valid @RequestBody ProjectTemplateRequest request,
                                                          @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.createProjectTemplate(request, user));
    }

    @PostMapping("/{id}/projects")
    public ResponseEntity<ProjectResponse> instantiate(@PathVariable Long id,
                                                       @Valid @RequestBody InstantiateProjectTemplateRequest request,
                                                       @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.instantiateProjectTemplate(id, request, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal User user) {
        templateService.deleteProjectTemplate(id, user);
        return ResponseEntity.noContent().build();
    }
}
