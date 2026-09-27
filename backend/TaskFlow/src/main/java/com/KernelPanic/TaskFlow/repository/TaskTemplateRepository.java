package com.KernelPanic.TaskFlow.repository;

import com.KernelPanic.TaskFlow.entity.TaskTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TaskTemplateRepository extends JpaRepository<TaskTemplate, Long> {
    List<TaskTemplate> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
    Optional<TaskTemplate> findByIdAndOwnerId(Long id, Long ownerId);
}
