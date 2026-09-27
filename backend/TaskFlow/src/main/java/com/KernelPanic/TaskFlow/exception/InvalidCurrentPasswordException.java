package com.KernelPanic.TaskFlow.exception;

/**
 * Lançada quando a senha atual informada na troca de senha não confere
 * com a senha armazenada do usuário.
 */
public class InvalidCurrentPasswordException extends RuntimeException {

    public InvalidCurrentPasswordException() {
        super("A senha atual informada está incorreta");
    }
}
