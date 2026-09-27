package com.KernelPanic.TaskFlow.repository;

import com.KernelPanic.TaskFlow.entity.TaskChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TaskChecklistItemRepository extends JpaRepository<TaskChecklistItem, Long> {
    Optional<TaskChecklistItem> findByIdAndTaskId(Long id, Long taskId);
    int countByTaskId(Long taskId);
}
