package com.KernelPanic.TaskFlow.controller;

import com.KernelPanic.TaskFlow.dto.ProjectForecastResponse;
import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.service.ProjectForecastService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("/api/projects/{projectId}/forecast") @RequiredArgsConstructor
public class ProjectForecastController {
    private final ProjectForecastService service;
    @GetMapping public ProjectForecastResponse forecast(@PathVariable Long projectId, @AuthenticationPrincipal User user) { return service.forecast(projectId, user); }
}