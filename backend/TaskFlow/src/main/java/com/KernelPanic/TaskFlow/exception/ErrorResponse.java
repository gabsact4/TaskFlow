package com.KernelPanic.TaskFlow.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Formato padrão de erro retornado pela API para facilitar o consumo pelo frontend.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        List<String> details
) {

    public ErrorResponse(int status, String error, String message) {
        this(Instant.now(), status, error, message, null);
    }

    public ErrorResponse(int status, String error, String message, List<String> details) {
        this(Instant.now(), status, error, message, details);
    }
}
