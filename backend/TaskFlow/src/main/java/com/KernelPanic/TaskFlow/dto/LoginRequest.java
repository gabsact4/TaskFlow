package com.KernelPanic.TaskFlow.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Corpo da requisição de autenticação (login) via e-mail e senha.
 */
public record LoginRequest(

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Informe um e-mail válido")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        String password
) {
}
