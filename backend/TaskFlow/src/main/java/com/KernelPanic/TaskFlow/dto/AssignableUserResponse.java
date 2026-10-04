package com.KernelPanic.TaskFlow.dto;

import com.KernelPanic.TaskFlow.entity.User;

public record AssignableUserResponse(Long id, String name, String role) {
    public static AssignableUserResponse fromEntity(User user) {
        return new AssignableUserResponse(user.getId(), user.getName(), user.getRole().name());
    }
}
