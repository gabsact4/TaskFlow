package com.KernelPanic.TaskFlow.repository;

import com.KernelPanic.TaskFlow.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    boolean existsByProjectKeyIgnoreCase(String projectKey);
    Optional<Project> findByProjectKeyIgnoreCase(String projectKey);
    List<Project> findByOwnerIdOrderByUpdatedAtDesc(Long ownerId);
    List<Project> findDistinctByMembers_IdOrderByUpdatedAtDesc(Long userId);
}
