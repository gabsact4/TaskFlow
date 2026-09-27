package com.KernelPanic.TaskFlow.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corpo da requisição para atualização dos dados de perfil do próprio usuário.
 */
public record UpdateProfileRequest(

        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 2, max = 120, message = "O nome deve ter entre 2 e 120 caracteres")
        String name,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Informe um e-mail válido")
        @Size(max = 180, message = "O e-mail deve ter no máximo 180 caracteres")
        String email
) {
}
