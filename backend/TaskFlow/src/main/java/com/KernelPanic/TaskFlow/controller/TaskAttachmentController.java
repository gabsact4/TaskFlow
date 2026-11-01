package com.KernelPanic.TaskFlow.controller;

import com.KernelPanic.TaskFlow.dto.TaskAttachmentResponse;
import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.service.TaskAttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController @RequestMapping("/api/tasks/{taskId}/attachments") @RequiredArgsConstructor
public class TaskAttachmentController {
    private final TaskAttachmentService service;
    @GetMapping public List<TaskAttachmentResponse> list(@PathVariable Long taskId, @AuthenticationPrincipal User user) { return service.list(taskId, user); }
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TaskAttachmentResponse> upload(@PathVariable Long taskId, @RequestPart("file") MultipartFile file, @AuthenticationPrincipal User user) {
        return ResponseEntity.status(201).body(service.upload(taskId, file, user));
    }
    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long taskId, @PathVariable Long attachmentId, @AuthenticationPrincipal User user) {
        var result = service.download(taskId, attachmentId, user);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(result.attachment().getContentType()))
                .contentLength(result.attachment().getSizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(result.attachment().getOriginalName(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff").body(result.resource());
    }
    @DeleteMapping("/{attachmentId}") public ResponseEntity<Void> delete(@PathVariable Long taskId, @PathVariable Long attachmentId, @AuthenticationPrincipal User user) {
        service.delete(taskId, attachmentId, user);
        return ResponseEntity.noContent().build();
    }
}