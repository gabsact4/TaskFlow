package com.KernelPanic.TaskFlow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 20)
        @Pattern(regexp = "[A-Za-z0-9_-]+", message = "a chave deve conter apenas letras, números, _ ou -")
        String projectKey,
        @Size(max = 1000) String description
) {}
