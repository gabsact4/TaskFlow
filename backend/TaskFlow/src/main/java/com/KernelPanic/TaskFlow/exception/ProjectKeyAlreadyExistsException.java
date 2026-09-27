package com.KernelPanic.TaskFlow.exception;

public class ProjectKeyAlreadyExistsException extends RuntimeException {
    public ProjectKeyAlreadyExistsException(String key) {
        super("Já existe um projeto com a chave: " + key);
    }
}
