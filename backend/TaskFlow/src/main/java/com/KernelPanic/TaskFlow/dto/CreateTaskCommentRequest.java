package com.KernelPanic.TaskFlow.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record CreateTaskCommentRequest(@NotBlank @Size(max = 2000) String content) {}