package com.KernelPanic.TaskFlow.service;

import com.KernelPanic.TaskFlow.dto.CreateProjectRequest;
import com.KernelPanic.TaskFlow.dto.ProjectResponse;
import com.KernelPanic.TaskFlow.dto.UpdateProjectRequest;
import com.KernelPanic.TaskFlow.entity.Project;
import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.exception.ProjectKeyAlreadyExistsException;
import com.KernelPanic.TaskFlow.exception.ProjectNotFoundException;
import com.KernelPanic.TaskFlow.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    @Transactional
    public ProjectResponse create(CreateProjectRequest request, User currentUser) {
        String key = normalizeKey(request.projectKey());

        if (projectRepository.existsByProjectKeyIgnoreCase(key)) {
            throw new ProjectKeyAlreadyExistsException(key);
        }

        Project project = Project.builder()
                .name(request.name().trim())
                .projectKey(key)
                .description(normalizeNullable(request.description()))
                .owner(currentUser)
                .build();

        return ProjectResponse.fromEntity(projectRepository.save(project));
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> list() {
        return projectRepository.findAll().stream()
                .map(ProjectResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listMine(User currentUser) {
        return projectRepository.findByOwnerIdOrderByUpdatedAtDesc(currentUser.getId())
                .stream()
                .map(ProjectResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public Project findEntity(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(Long id) {
        return ProjectResponse.fromEntity(findEntity(id));
    }

    @Transactional
    public ProjectResponse update(Long id, UpdateProjectRequest request, User currentUser) {
        Project project = findEntity(id);
        ensureOwner(project, currentUser);

        project.setName(request.name().trim());
        project.setDescription(normalizeNullable(request.description()));
        if (request.status() != null) {
            project.setStatus(request.status());
        }

        return ProjectResponse.fromEntity(projectRepository.save(project));
    }

    @Transactional
    public void delete(Long id, User currentUser) {
        Project project = findEntity(id);
        ensureOwner(project, currentUser);
        projectRepository.delete(project);
    }

    public void ensureOwner(Project project, User currentUser) {
        if (!project.getOwner().getId().equals(currentUser.getId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Somente o proprietário do projeto pode realizar esta operação.");
        }
    }

    private String normalizeKey(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
