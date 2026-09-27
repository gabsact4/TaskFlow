package com.KernelPanic.TaskFlow.exception;

/**
 * Lançada ao tentar cadastrar um usuário com um e-mail já existente.
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("Já existe uma conta cadastrada com o e-mail informado: " + email);
    }
}
