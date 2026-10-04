package com.KernelPanic.TaskFlow.repository;

import com.KernelPanic.TaskFlow.entity.ProjectTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProjectTemplateRepository extends JpaRepository<ProjectTemplate, Long> {
    List<ProjectTemplate> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
    List<ProjectTemplate> findAllByOrderByCreatedAtDesc();
    Optional<ProjectTemplate> findByIdAndOwnerId(Long id, Long ownerId);
}
