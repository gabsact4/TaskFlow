package com.KernelPanic.TaskFlow.service;

import com.KernelPanic.TaskFlow.dto.CreateProjectRequest;
import com.KernelPanic.TaskFlow.dto.ProjectResponse;
import com.KernelPanic.TaskFlow.dto.UpdateProjectRequest;
import com.KernelPanic.TaskFlow.entity.Project;
import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.exception.ProjectKeyAlreadyExistsException;
import com.KernelPanic.TaskFlow.exception.ProjectNotFoundException;
import com.KernelPanic.TaskFlow.repository.ProjectRepository;
import com.KernelPanic.TaskFlow.repository.TaskRepository;
import com.KernelPanic.TaskFlow.repository.UserRepository;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ChangeHistoryService historyService;

    @Transactional
    public ProjectResponse create(CreateProjectRequest request, User currentUser) {
        if (currentUser.getRole() == com.KernelPanic.TaskFlow.enums.Role.DEV
            || currentUser.getRole() == com.KernelPanic.TaskFlow.enums.Role.USER) {
            throw new org.springframework.security.access.AccessDeniedException("Dev não pode criar projetos.");
        }
        String key = normalizeKey(request.projectKey());

        if (projectRepository.existsByProjectKeyIgnoreCase(key)) {
            throw new ProjectKeyAlreadyExistsException(key);
        }

        Project project = Project.builder()
                .name(request.name().trim())
                .projectKey(key)
                .description(normalizeNullable(request.description()))
                .owner(currentUser)
                .members(resolveMembers(request.memberIds(), currentUser))
                .build();

        Project saved = projectRepository.save(project);
        historyService.record(saved, null, currentUser, "PROJECT", saved.getId(), "CREATED",
            "criou o projeto \"" + saved.getName() + "\"", java.util.Map.of("name", saved.getName(), "projectKey", saved.getProjectKey()));
        return ProjectResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> list() {
        return projectRepository.findAll().stream()
                .map(ProjectResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> list(User currentUser) {
        if (isMaster(currentUser)) return list();
        if (currentUser.getRole() == com.KernelPanic.TaskFlow.enums.Role.DEV
            || currentUser.getRole() == com.KernelPanic.TaskFlow.enums.Role.USER) {
            java.util.LinkedHashMap<Long, Project> visible = new java.util.LinkedHashMap<>();
            projectRepository.findDistinctByMembers_IdOrderByUpdatedAtDesc(currentUser.getId()).forEach(p -> visible.put(p.getId(), p));
            taskRepository.findProjectsByAssigneeId(currentUser.getId()).forEach(p -> visible.put(p.getId(), p));
            return visible.values().stream().map(ProjectResponse::fromEntity).toList();
        }
        java.util.LinkedHashMap<Long, Project> visible = new java.util.LinkedHashMap<>();
        projectRepository.findByOwnerIdOrderByUpdatedAtDesc(currentUser.getId()).forEach(p -> visible.put(p.getId(), p));
        projectRepository.findDistinctByMembers_IdOrderByUpdatedAtDesc(currentUser.getId()).forEach(p -> visible.put(p.getId(), p));
        return visible.values().stream().map(ProjectResponse::fromEntity).toList();
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
    public ProjectResponse get(Long id, User currentUser) {
        Project project = findEntity(id);
        boolean assignedDev = (currentUser.getRole() == com.KernelPanic.TaskFlow.enums.Role.DEV
            || currentUser.getRole() == com.KernelPanic.TaskFlow.enums.Role.USER)
                && taskRepository.existsByProjectIdAndAssigneeId(id, currentUser.getId());
        boolean projectMember = project.getMembers().stream().anyMatch(member -> Objects.equals(member.getId(), currentUser.getId()));
        if (!isMaster(currentUser) && !project.getOwner().getId().equals(currentUser.getId()) && !projectMember && !assignedDev) {
            throw new org.springframework.security.access.AccessDeniedException("Você não tem acesso a este projeto.");
        }
        return ProjectResponse.fromEntity(project);
    }

    @Transactional
    public ProjectResponse update(Long id, UpdateProjectRequest request, User currentUser) {
        Project project = findEntity(id);
        ensureOwner(project, currentUser);

        java.util.Map<String, Object> before = new java.util.LinkedHashMap<>();
        before.put("name", project.getName()); before.put("description", project.getDescription()); before.put("status", project.getStatus());
        before.put("memberIds", project.getMembers().stream().map(member -> member.getId()).toList());

        project.setName(request.name().trim());
        project.setDescription(normalizeNullable(request.description()));
        if (request.status() != null) {
            project.setStatus(request.status());
        }
        if (request.memberIds() != null) {
            project.setMembers(resolveMembers(request.memberIds(), project.getOwner()));
        }

        Project saved = projectRepository.save(project);
        java.util.Map<String, Object> after = new java.util.LinkedHashMap<>();
        after.put("name", saved.getName()); after.put("description", saved.getDescription()); after.put("status", saved.getStatus());
        after.put("memberIds", saved.getMembers().stream().map(member -> member.getId()).toList());
        historyService.record(saved, null, currentUser, "PROJECT", saved.getId(), "UPDATED",
            "atualizou o projeto \"" + saved.getName() + "\"", java.util.Map.of("before", before, "after", after));
        return ProjectResponse.fromEntity(saved);
    }

    @Transactional
    public void delete(Long id, User currentUser) {
        Project project = findEntity(id);
        ensureOwner(project, currentUser);
        historyService.record(project, null, currentUser, "PROJECT", project.getId(), "DELETED",
            "excluiu o projeto \"" + project.getName() + "\"", java.util.Map.of("name", project.getName(), "projectKey", project.getProjectKey()));
        projectRepository.delete(project);
    }

    public void ensureOwner(Project project, User currentUser) {
        if (!isMaster(currentUser)
            && (currentUser.getRole() != com.KernelPanic.TaskFlow.enums.Role.PO
                || !project.getOwner().getId().equals(currentUser.getId()))) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Somente o proprietário do projeto pode realizar esta operação.");
        }
    }

    private Set<User> resolveMembers(List<Long> memberIds, User owner) {
        Set<User> members = new HashSet<>();
        if (memberIds != null) {
            for (Long id : memberIds) {
                if (id == null) continue;
                User member = userRepository.findById(id).orElseThrow(() -> new com.KernelPanic.TaskFlow.exception.UserNotFoundException(id));
                members.add(member);
            }
        }
        members.removeIf(member -> Objects.equals(member.getId(), owner.getId()));
        return members;
    }

    public boolean isMember(Project project, Long userId) {
        return project.getOwner().getId().equals(userId)
                || project.getMembers().stream().anyMatch(member -> Objects.equals(member.getId(), userId));
    }

    private boolean isMaster(User user) { return user.getRole() == com.KernelPanic.TaskFlow.enums.Role.MASTER || user.getRole() == com.KernelPanic.TaskFlow.enums.Role.ADMIN; }

    private String normalizeKey(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
