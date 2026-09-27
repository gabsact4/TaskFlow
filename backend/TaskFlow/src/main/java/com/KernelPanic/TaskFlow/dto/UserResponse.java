package com.KernelPanic.TaskFlow.dto;

import com.KernelPanic.TaskFlow.entity.User;

/**
 * Representação pública do usuário, sem dados sensíveis (nunca inclui a senha).
 */
public record UserResponse(
        Long id,
        String name,
        String email,
        String role
) {

    public static UserResponse fromEntity(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole().name());
    }
}
