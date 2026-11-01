package com.KernelPanic.TaskFlow.controller;

import com.KernelPanic.TaskFlow.dto.TeamCapacityResponse;
import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.service.TeamAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController @RequestMapping("/api/team") @RequiredArgsConstructor
public class TeamAnalyticsController {
    private final TeamAnalyticsService service;
    @GetMapping("/capacity")
    public TeamCapacityResponse capacity(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                         @AuthenticationPrincipal User user) { return service.capacity(from, to, user); }
}