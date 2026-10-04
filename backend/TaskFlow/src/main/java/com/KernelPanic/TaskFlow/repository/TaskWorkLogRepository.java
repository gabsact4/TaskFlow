package com.KernelPanic.TaskFlow.repository;

import com.KernelPanic.TaskFlow.entity.TaskWorkLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskWorkLogRepository extends JpaRepository<TaskWorkLog, Long> {
    List<TaskWorkLog> findByTaskIdOrderByCreatedAtDesc(Long taskId);
}
