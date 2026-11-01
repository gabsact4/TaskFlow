package com.KernelPanic.TaskFlow.controller;

import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.service.ExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@RestController @RequestMapping("/api/exports") @RequiredArgsConstructor
public class ExportController {
    private final ExportService service;
    @GetMapping public ResponseEntity<byte[]> export(@RequestParam(defaultValue = "json") String format,
            @RequestParam(required = false) Long projectId, @AuthenticationPrincipal User user) {
        String normalized = format.toLowerCase(java.util.Locale.ROOT);
        if (!normalized.equals("json") && !normalized.equals("csv")) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Formato de exportação aceito: csv ou json.");
        String stamp = Instant.now().toString().replace(":", "-");
        byte[] content = normalized.equals("csv") ? service.csv(user, projectId) : service.json(user, projectId);
        MediaType type = normalized.equals("csv") ? new MediaType("text", "csv", StandardCharsets.UTF_8) : MediaType.APPLICATION_JSON;
        return ResponseEntity.ok().contentType(type).contentLength(content.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("taskflow-" + stamp + "." + normalized, StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff").body(content);
    }
}