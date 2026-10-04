package com.KernelPanic.TaskFlow.repository;

import com.KernelPanic.TaskFlow.entity.Task;
import com.KernelPanic.TaskFlow.entity.Project;
import org.springframework.data.jpa.repository.Query;
import com.KernelPanic.TaskFlow.enums.TaskStatus;
import com.KernelPanic.TaskFlow.enums.TaskRecurrence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByProjectIdOrderByCreatedAtDesc(Long projectId);
    List<Task> findByProjectIdAndStatusOrderByCreatedAtDesc(Long projectId, TaskStatus status);
    @Query("select distinct t.project from Task t where t.assignee.id = :assigneeId")
    List<Project> findProjectsByAssigneeId(Long assigneeId);
    boolean existsByProjectIdAndAssigneeId(Long projectId, Long assigneeId);
    List<Task> findByProjectIdAndAssigneeIdOrderByCreatedAtDesc(Long projectId, Long assigneeId);
    List<Task> findByRecurrenceNotAndNextOccurrenceDateLessThanEqual(TaskRecurrence recurrence, LocalDate date);

    @Query("select t from Task t join fetch t.project p join fetch p.owner left join fetch t.assignee left join fetch t.creator where t.dueDate is not null and t.status <> :done and t.parentTask is null")
    List<Task> findOpenTopLevelTasksWithDueDate(@org.springframework.data.repository.query.Param("done") TaskStatus done);
}
