package com.KernelPanic.TaskFlow.exception;

public class InvalidWebAuthnException extends RuntimeException {

    public InvalidWebAuthnException() {
        super("Cerimônia de passkey inválida ou expirada");
    }
}