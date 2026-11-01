package com.KernelPanic.TaskFlow.controller;

import com.KernelPanic.TaskFlow.dto.FocusSessionRequest;
import com.KernelPanic.TaskFlow.dto.FocusSessionResponse;
import com.KernelPanic.TaskFlow.dto.FinishFocusSessionRequest;
import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.service.FocusSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/productivity/focus") @RequiredArgsConstructor
public class FocusSessionController {
    private final FocusSessionService service;
    @GetMapping("/active") public ResponseEntity<FocusSessionResponse> active(@AuthenticationPrincipal User user) {
        FocusSessionResponse active = service.active(user);
        return active == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(active);
    }
    @GetMapping("/history") public List<FocusSessionResponse> history(@AuthenticationPrincipal User user) { return service.history(user); }
    @PostMapping public ResponseEntity<FocusSessionResponse> start(@Valid @RequestBody FocusSessionRequest request, @AuthenticationPrincipal User user) { return ResponseEntity.status(201).body(service.start(request, user)); }
    @PostMapping("/{id}/finish") public FocusSessionResponse finish(@PathVariable Long id, @Valid @RequestBody FinishFocusSessionRequest request, @AuthenticationPrincipal User user) { return service.finish(id, request, user); }
}