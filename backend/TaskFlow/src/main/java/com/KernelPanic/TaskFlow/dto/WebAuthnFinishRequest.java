package com.KernelPanic.TaskFlow.dto;

import jakarta.validation.constraints.NotBlank;

public record WebAuthnFinishRequest(
        @NotBlank String challengeId,
        @NotBlank String credential
) {
}