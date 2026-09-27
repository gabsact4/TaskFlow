package com.KernelPanic.TaskFlow.dto;

import com.KernelPanic.TaskFlow.entity.TaskChecklistItem;

public record TaskChecklistItemResponse(Long id, String text, boolean done, int position) {
    public static TaskChecklistItemResponse fromEntity(TaskChecklistItem item) {
        return new TaskChecklistItemResponse(item.getId(), item.getText(), item.isDone(), item.getPosition());
    }
}
