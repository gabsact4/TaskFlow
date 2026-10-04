package com.KernelPanic.TaskFlow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateProjectRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 20)
        @Pattern(regexp = "[A-Za-z0-9_-]+", message = "a chave deve conter apenas letras, números, _ ou -")
        String projectKey,
        @Size(max = 1000) String description,
        List<Long> memberIds
) {
    public CreateProjectRequest(String name, String projectKey, String description) { this(name, projectKey, description, List.of()); }
}
