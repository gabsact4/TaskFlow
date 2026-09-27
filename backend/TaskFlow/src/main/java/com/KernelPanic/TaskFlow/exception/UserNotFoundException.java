package com.KernelPanic.TaskFlow.exception;

/**
 * Lançada quando um usuário referenciado por id não existe.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long id) {
        super("Usuário não encontrado com id: " + id);
    }
}
