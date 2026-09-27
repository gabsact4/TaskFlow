package com.KernelPanic.TaskFlow.dto;

import java.util.Map;

public record WebAuthnOptionsResponse(String challengeId, Map<String, Object> options) {
}