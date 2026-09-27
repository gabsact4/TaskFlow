package com.KernelPanic.TaskFlow.exception;

public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(Long id) {
        super("Tarefa não encontrada: " + id);
    }
}
