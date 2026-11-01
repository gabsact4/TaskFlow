package com.KernelPanic.TaskFlow.repository;
import com.KernelPanic.TaskFlow.entity.TaskAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface TaskAttachmentRepository extends JpaRepository<TaskAttachment, Long> {
    List<TaskAttachment> findByTaskIdOrderByCreatedAtDesc(Long taskId);
    Optional<TaskAttachment> findByIdAndTaskId(Long id, Long taskId);
}