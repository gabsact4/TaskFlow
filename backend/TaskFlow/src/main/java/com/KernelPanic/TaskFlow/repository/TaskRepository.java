package com.KernelPanic.TaskFlow.repository;

import com.KernelPanic.TaskFlow.entity.Task;
import com.KernelPanic.TaskFlow.enums.TaskStatus;
import com.KernelPanic.TaskFlow.enums.TaskRecurrence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByProjectIdOrderByCreatedAtDesc(Long projectId);
    List<Task> findByProjectIdAndStatusOrderByCreatedAtDesc(Long projectId, TaskStatus status);
    List<Task> findByRecurrenceNotAndNextOccurrenceDateLessThanEqual(TaskRecurrence recurrence, LocalDate date);
}
