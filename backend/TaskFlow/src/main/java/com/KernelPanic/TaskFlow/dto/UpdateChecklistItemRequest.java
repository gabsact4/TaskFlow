package com.KernelPanic.TaskFlow.dto;

import jakarta.validation.constraints.Size;

public record UpdateChecklistItemRequest(
        @Size(max = 300) String text,
        Boolean done
) {}
