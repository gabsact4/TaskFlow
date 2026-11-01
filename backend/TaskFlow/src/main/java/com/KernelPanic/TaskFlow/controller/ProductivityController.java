package com.KernelPanic.TaskFlow.controller;

import com.KernelPanic.TaskFlow.dto.ProductivityGoalRequest;
import com.KernelPanic.TaskFlow.dto.ProductivityGoalResponse;
import com.KernelPanic.TaskFlow.dto.ProductivityReportResponse;
import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.service.ProductivityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController @RequestMapping("/api/productivity") @RequiredArgsConstructor
public class ProductivityController {
    private final ProductivityService service;
    @GetMapping("/report") public ProductivityReportResponse report(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to, @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long projectId, @AuthenticationPrincipal User user) { return service.report(from, to, userId, projectId, user); }
    @GetMapping("/goals") public List<ProductivityGoalResponse> goals(@AuthenticationPrincipal User user) { return service.goals(user); }
    @PostMapping("/goals") public ProductivityGoalResponse saveGoal(@Valid @RequestBody ProductivityGoalRequest request, @AuthenticationPrincipal User user) { return service.saveGoal(request, user); }
    @DeleteMapping("/goals/{id}") public ResponseEntity<Void> deleteGoal(@PathVariable Long id, @AuthenticationPrincipal User user) { service.deleteGoal(id, user); return ResponseEntity.noContent().build(); }
}