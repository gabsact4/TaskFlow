package com.KernelPanic.TaskFlow.controller;

import com.KernelPanic.TaskFlow.dto.ChangeHistoryResponse;
import com.KernelPanic.TaskFlow.dto.PageResponse;
import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.service.ChangeHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/history") @RequiredArgsConstructor
public class ChangeHistoryController {
    private final ChangeHistoryService service;
    @GetMapping public PageResponse<ChangeHistoryResponse> list(@RequestParam(required = false) Long projectId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "30") int size,
            @AuthenticationPrincipal User user) { return service.list(projectId, page, size, user); }
}