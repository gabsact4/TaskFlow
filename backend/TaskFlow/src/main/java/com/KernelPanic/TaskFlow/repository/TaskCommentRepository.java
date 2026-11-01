package com.KernelPanic.TaskFlow.repository;
import com.KernelPanic.TaskFlow.entity.TaskComment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {
    List<TaskComment> findByTaskIdOrderByCreatedAtAsc(Long taskId);
}