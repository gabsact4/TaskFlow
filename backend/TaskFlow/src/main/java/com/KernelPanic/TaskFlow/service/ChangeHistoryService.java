package com.KernelPanic.TaskFlow.service;

import com.KernelPanic.TaskFlow.dto.ChangeHistoryResponse;
import com.KernelPanic.TaskFlow.dto.PageResponse;
import com.KernelPanic.TaskFlow.entity.ChangeHistory;
import com.KernelPanic.TaskFlow.entity.Project;
import com.KernelPanic.TaskFlow.entity.Task;
import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.enums.Role;
import com.KernelPanic.TaskFlow.exception.ProjectNotFoundException;
import com.KernelPanic.TaskFlow.repository.ChangeHistoryRepository;
import com.KernelPanic.TaskFlow.repository.ProjectRepository;
import com.KernelPanic.TaskFlow.repository.TaskRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service @RequiredArgsConstructor
public class ChangeHistoryService {
    private final ChangeHistoryRepository history;
    private final ObjectMapper objectMapper;
    private final ProjectRepository projects;
    private final TaskRepository tasks;

    @Transactional
    public void record(Project project, Task task, User actor, String entityType, Long entityId, String action, String summary, Map<String, ?> details) {
        if (project == null || actor == null) return;
        String json = null;
        if (details != null && !details.isEmpty()) {
            try { json = objectMapper.writeValueAsString(details); }
            catch (JsonProcessingException e) { throw new IllegalStateException("Não foi possível registrar detalhes do histórico.", e); }
        }
        history.save(ChangeHistory.builder().projectId(project.getId()).projectOwnerId(project.getOwner().getId())
                .actorId(actor.getId()).actorName(actor.getName()).entityType(entityType).entityId(entityId)
                .taskId(task == null ? null : task.getId()).action(action).summary(summary).detailsJson(json).build());
    }

    @Transactional(readOnly = true)
    public PageResponse<ChangeHistoryResponse> list(Long projectId, int page, int size, User user) {
        if (page < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A página deve ser maior ou igual a zero.");
        int pageSize = Math.clamp(size, 1, 100);
        var pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
        if (isMaster(user)) {
            if (projectId != null && !projects.existsById(projectId) && !history.existsByProjectId(projectId)) throw new ProjectNotFoundException(projectId);
            var result = projectId == null ? history.findAll(pageable) : history.findByProjectIdOrderByCreatedAtDesc(projectId, pageable);
            return PageResponse.from(result.map(ChangeHistoryResponse::fromEntity));
        }
        if (projectId != null) {
            var project = projects.findById(projectId).orElse(null);
            if (project == null && (user.getRole() != Role.PO || !history.existsByProjectIdAndProjectOwnerId(projectId, user.getId()))) throw new ProjectNotFoundException(projectId);
            boolean deletedProjectOwner = project == null && user.getRole() == Role.PO && history.existsByProjectIdAndProjectOwnerId(projectId, user.getId());
            if (deletedProjectOwner) return PageResponse.from(history.findByProjectIdOrderByCreatedAtDesc(projectId, pageable).map(ChangeHistoryResponse::fromEntity));
            if (!canAccessProject(project, user)) throw new AccessDeniedException("Você não tem acesso ao histórico deste projeto.");
            boolean fullAccess = user.getRole() == Role.PO && project.getOwner().getId().equals(user.getId());
            List<Long> fullIds = fullAccess ? List.of(projectId) : List.of(-1L);
            List<Long> taskIds = visibleTaskIds(projectId, user, fullAccess);
            return PageResponse.from(history.findAccessible(List.of(projectId), fullIds, nonEmpty(taskIds), false, user.getId(), pageable).map(ChangeHistoryResponse::fromEntity));
        }
        List<Long> visibleProjectIds = visibleProjectIds(user);
        List<Long> fullIds = user.getRole() == Role.PO ? projects.findByOwnerIdOrderByUpdatedAtDesc(user.getId()).stream().map(project -> project.getId()).toList() : List.of();
        LinkedHashSet<Long> taskIds = new LinkedHashSet<>();
        for (Long visibleProjectId : visibleProjectIds) taskIds.addAll(visibleTaskIds(visibleProjectId, user, fullIds.contains(visibleProjectId)));
        var result = history.findAccessible(nonEmpty(visibleProjectIds), nonEmpty(fullIds), nonEmpty(new ArrayList<>(taskIds)),
                user.getRole() == Role.PO, user.getId(), pageable);
        return PageResponse.from(result.map(ChangeHistoryResponse::fromEntity));
    }

    @Transactional(readOnly = true)
    public List<ChangeHistory> exportForProjects(List<Long> projectIds, List<Long> taskIds, User user, boolean includeAllMasterHistory) {
        if (isMaster(user)) return includeAllMasterHistory
                ? history.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                : projectIds.isEmpty() ? List.of() : history.findByProjectIdInOrderByCreatedAtDesc(projectIds);
        List<Long> fullIds = user.getRole() == Role.PO ? projectIds.stream().filter(id -> projects.findById(id).map(project -> project.getOwner().getId().equals(user.getId())).orElse(false)).toList() : List.of();
        return history.findAccessibleForExport(nonEmpty(projectIds), nonEmpty(fullIds), nonEmpty(taskIds), user.getRole() == Role.PO, user.getId());
    }

    private List<Long> visibleProjectIds(User user) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        projects.findDistinctByMembers_IdOrderByUpdatedAtDesc(user.getId()).forEach(project -> ids.add(project.getId()));
        if (user.getRole() == Role.PO) projects.findByOwnerIdOrderByUpdatedAtDesc(user.getId()).forEach(project -> ids.add(project.getId()));
        if (user.getRole() == Role.DEV || user.getRole() == Role.USER) tasks.findProjectsByAssigneeId(user.getId()).forEach(project -> ids.add(project.getId()));
        return new ArrayList<>(ids);
    }

    private List<Long> visibleTaskIds(Long projectId, User user, boolean fullAccess) {
        return tasks.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .filter(task -> fullAccess || (task.getAssignee() != null && task.getAssignee().getId().equals(user.getId())))
                .map(task -> task.getId()).toList();
    }

    private boolean canAccessProject(com.KernelPanic.TaskFlow.entity.Project project, User user) {
        return project.getOwner().getId().equals(user.getId())
                || project.getMembers().stream().anyMatch(member -> member.getId().equals(user.getId()))
                || ((user.getRole() == Role.DEV || user.getRole() == Role.USER) && tasks.existsByProjectIdAndAssigneeId(project.getId(), user.getId()));
    }

    private List<Long> nonEmpty(List<Long> ids) { return ids.isEmpty() ? List.of(-1L) : ids; }

    private boolean isMaster(User user) { return user.getRole() == Role.MASTER || user.getRole() == Role.ADMIN; }
}