package com.KernelPanic.TaskFlow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corpo da requisição para troca de senha do próprio usuário.
 * Exige a senha atual para confirmar a identidade de quem está fazendo a alteração.
 */
public record ChangePasswordRequest(

        @NotBlank(message = "A senha atual é obrigatória")
        String currentPassword,

        @NotBlank(message = "A nova senha é obrigatória")
        @Size(min = 8, max = 72, message = "A nova senha deve ter entre 8 e 72 caracteres")
        String newPassword
) {
}
