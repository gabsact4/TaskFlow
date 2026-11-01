package com.KernelPanic.TaskFlow.controller;

import com.KernelPanic.TaskFlow.dto.CreateAbsenceRequest;
import com.KernelPanic.TaskFlow.dto.ReviewAbsenceRequest;
import com.KernelPanic.TaskFlow.dto.UserAbsenceResponse;
import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.service.UserAbsenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/absences") @RequiredArgsConstructor
public class UserAbsenceController {
    private final UserAbsenceService service;
    @GetMapping public List<UserAbsenceResponse> list(@AuthenticationPrincipal User user) { return service.list(user); }
    @PostMapping public ResponseEntity<UserAbsenceResponse> request(@Valid @RequestBody CreateAbsenceRequest request, @AuthenticationPrincipal User user) {
        return ResponseEntity.status(201).body(service.request(request, user));
    }
    @PatchMapping("/{id}/review") public UserAbsenceResponse review(@PathVariable Long id, @Valid @RequestBody ReviewAbsenceRequest request, @AuthenticationPrincipal User user) { return service.review(id, request, user); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> cancel(@PathVariable Long id, @AuthenticationPrincipal User user) {
        service.cancel(id, user);
        return ResponseEntity.noContent().build();
    }
}