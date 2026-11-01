package com.KernelPanic.TaskFlow.controller;

import com.KernelPanic.TaskFlow.dto.StartTaskTimerRequest;
import com.KernelPanic.TaskFlow.dto.TaskTimerResponse;
import com.KernelPanic.TaskFlow.dto.TaskWorkLogResponse;
import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.service.TaskTimerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/productivity/timer") @RequiredArgsConstructor
public class TaskTimerController {
    private final TaskTimerService service;
    @GetMapping("/active") public ResponseEntity<TaskTimerResponse> active(@AuthenticationPrincipal User user) {
        TaskTimerResponse active = service.active(user);
        return active == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(active);
    }
    @PostMapping("/start") public TaskTimerResponse start(@Valid @RequestBody StartTaskTimerRequest request, @AuthenticationPrincipal User user) { return service.start(request, user); }
    @PostMapping("/pause") public TaskTimerResponse pause(@AuthenticationPrincipal User user) { return service.pause(user); }
    @PostMapping("/resume") public TaskTimerResponse resume(@AuthenticationPrincipal User user) { return service.resume(user); }
    @PostMapping("/stop") public TaskWorkLogResponse stop(@RequestParam(required = false) String comment, @AuthenticationPrincipal User user) { return service.stop(user, comment); }
}