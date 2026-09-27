package com.KernelPanic.TaskFlow.service;

import com.KernelPanic.TaskFlow.entity.Task;
import com.KernelPanic.TaskFlow.entity.TaskChecklistItem;
import com.KernelPanic.TaskFlow.enums.TaskRecurrence;
import com.KernelPanic.TaskFlow.enums.TaskStatus;
import com.KernelPanic.TaskFlow.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TaskRecurrenceScheduler {

    private final TaskRepository taskRepository;

    @Scheduled(cron = "${taskflow.recurrence.cron:0 10 0 * * *}", zone = "UTC")
    @Transactional
    public void createDueOccurrences() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<Task> schedules = taskRepository.findByRecurrenceNotAndNextOccurrenceDateLessThanEqual(TaskRecurrence.NONE, today);
        List<Task> occurrences = new ArrayList<>();

        for (Task schedule : schedules) {
            LocalDate date = schedule.getNextOccurrenceDate();
            while (date != null && !date.isAfter(today)
                    && (schedule.getRecurrenceEndDate() == null || !date.isAfter(schedule.getRecurrenceEndDate()))) {
                Task occurrence = Task.builder()
                        .title(schedule.getTitle())
                        .description(schedule.getDescription())
                        .status(TaskStatus.TODO)
                        .priority(schedule.getPriority())
                        .dueDate(date)
                        .project(schedule.getProject())
                        .assignee(schedule.getAssignee())
                        .creator(schedule.getCreator())
                        .parentTask(schedule.getParentTask())
                        .build();
                List<TaskChecklistItem> checklist = schedule.getChecklistItems().stream()
                        .map(item -> TaskChecklistItem.builder()
                                .task(occurrence)
                                .text(item.getText())
                                .position(item.getPosition())
                                .done(false)
                                .build())
                        .toList();
                occurrence.getChecklistItems().addAll(checklist);
                occurrences.add(occurrence);
                date = schedule.getRecurrence().next(date);
            }

            if (date != null && schedule.getRecurrenceEndDate() != null && date.isAfter(schedule.getRecurrenceEndDate())) {
                schedule.setRecurrence(TaskRecurrence.NONE);
                schedule.setNextOccurrenceDate(null);
                schedule.setRecurrenceEndDate(null);
            } else {
                schedule.setNextOccurrenceDate(date);
            }
        }
        taskRepository.saveAll(occurrences);
    }
}
