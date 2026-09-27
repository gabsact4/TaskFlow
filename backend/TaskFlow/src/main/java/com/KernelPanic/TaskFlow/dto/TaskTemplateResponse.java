package com.KernelPanic.TaskFlow.dto;

import com.KernelPanic.TaskFlow.entity.TaskTemplate;
import java.time.LocalDate;

public record TaskTemplateResponse(Long id, String name, String title, String description,
                                   String priority, String recurrence, LocalDate recurrenceEndDate,
                                   String checklistItems) {
    public static TaskTemplateResponse fromEntity(TaskTemplate template) {
        return new TaskTemplateResponse(template.getId(), template.getName(), template.getTitle(),
                template.getDescription(), template.getPriority().name(), template.getRecurrence().name(),
                template.getRecurrenceEndDate(),
                template.getChecklistItems());
    }
}
