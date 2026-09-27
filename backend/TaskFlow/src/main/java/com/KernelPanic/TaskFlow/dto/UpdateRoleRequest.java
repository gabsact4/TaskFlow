package com.KernelPanic.TaskFlow.dto;

import com.KernelPanic.TaskFlow.enums.Role;
import jakarta.validation.constraints.NotNull;

/**
 * Corpo da requisição usada por um administrador para alterar o papel de um usuário.
 */
public record UpdateRoleRequest(

        @NotNull(message = "O papel (role) é obrigatório")
        Role role
) {
}
