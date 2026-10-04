package com.KernelPanic.TaskFlow.service;

import com.KernelPanic.TaskFlow.dto.*;
import com.KernelPanic.TaskFlow.entity.ProjectTemplate;
import com.KernelPanic.TaskFlow.entity.TaskTemplate;
import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.enums.TaskPriority;
import com.KernelPanic.TaskFlow.enums.TaskRecurrence;
import com.KernelPanic.TaskFlow.enums.TaskStatus;
import com.KernelPanic.TaskFlow.enums.Role;
import com.KernelPanic.TaskFlow.repository.ProjectTemplateRepository;
import com.KernelPanic.TaskFlow.repository.TaskTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TemplateService {
    private final TaskTemplateRepository taskTemplates;
    private final ProjectTemplateRepository projectTemplates;
    private final TaskService taskService;
    private final ProjectService projectService;

    @Transactional(readOnly = true)
    public List<TaskTemplateResponse> listTaskTemplates(User user) {
        List<TaskTemplate> templates = isMaster(user)
            ? taskTemplates.findAllByOrderByCreatedAtDesc()
            : taskTemplates.findByOwnerIdOrderByCreatedAtDesc(user.getId());
        return templates.stream().map(TaskTemplateResponse::fromEntity).toList();
    }

    @Transactional
    public TaskTemplateResponse createTaskTemplate(TaskTemplateRequest request, User user) {
        ensureCanManageTemplates(user);
        TaskTemplate template = TaskTemplate.builder()
                .name(request.name().trim())
                .title(request.title().trim())
                .description(normalize(request.description()))
                .priority(request.priority() == null ? TaskPriority.MEDIUM : request.priority())
                .recurrence(request.recurrence() == null ? TaskRecurrence.NONE : request.recurrence())
                .recurrenceEndDate(request.recurrence() == null || request.recurrence() == TaskRecurrence.NONE ? null : request.recurrenceEndDate())
                .checklistItems(normalize(request.checklistItems()))
                .owner(user)
                .build();
        return TaskTemplateResponse.fromEntity(taskTemplates.save(template));
    }

    @Transactional
    public void deleteTaskTemplate(Long id, User user) {
        TaskTemplate template = findTaskTemplate(id, user);
        taskTemplates.delete(template);
    }

    @Transactional
    public TaskResponse instantiateTaskTemplate(Long id, InstantiateTaskTemplateRequest request, User user) {
        TaskTemplate template = findTaskTemplate(id, user);
        TaskResponse created = taskService.create(new CreateTaskRequest(
                template.getTitle(), template.getDescription(), TaskStatus.TODO, template.getPriority(),
                request.dueDate(), request.projectId(), request.assigneeId(), request.parentTaskId(),
                template.getRecurrence(), template.getRecurrenceEndDate()), user);
        if (template.getChecklistItems() != null) {
            for (String item : template.getChecklistItems().lines().map(line -> line.trim()).filter(item -> !item.isEmpty()).toList()) {
                taskService.addChecklistItem(created.id(), new CreateChecklistItemRequest(item), user);
            }
        }
        return taskService.get(created.id());
    }

    @Transactional(readOnly = true)
    public List<ProjectTemplateResponse> listProjectTemplates(User user) {
        List<ProjectTemplate> templates = isMaster(user)
            ? projectTemplates.findAllByOrderByCreatedAtDesc()
            : projectTemplates.findByOwnerIdOrderByCreatedAtDesc(user.getId());
        return templates.stream().map(ProjectTemplateResponse::fromEntity).toList();
    }

    @Transactional
    public ProjectTemplateResponse createProjectTemplate(ProjectTemplateRequest request, User user) {
        ensureCanManageTemplates(user);
        ProjectTemplate template = ProjectTemplate.builder()
                .name(request.name().trim())
                .projectName(request.projectName().trim())
                .description(normalize(request.description()))
                .starterTasks(normalize(request.starterTasks()))
                .owner(user)
                .build();
        return ProjectTemplateResponse.fromEntity(projectTemplates.save(template));
    }

    @Transactional
    public void deleteProjectTemplate(Long id, User user) {
        ProjectTemplate template = findProjectTemplate(id, user);
        projectTemplates.delete(template);
    }

    @Transactional
    public ProjectResponse instantiateProjectTemplate(Long id, InstantiateProjectTemplateRequest request, User user) {
        ProjectTemplate template = findProjectTemplate(id, user);
        ProjectResponse project = projectService.create(new CreateProjectRequest(template.getProjectName(), request.projectKey(), template.getDescription()), user);
        if (template.getStarterTasks() == null || template.getStarterTasks().isBlank()) return project;

        List<Long> taskIdsByDepth = new ArrayList<>();
        for (String rawLine : template.getStarterTasks().lines().toList()) {
            if (rawLine.isBlank()) continue;
            int depth = indentation(rawLine);
            while (taskIdsByDepth.size() > depth) taskIdsByDepth.remove(taskIdsByDepth.size() - 1);
            if (depth > taskIdsByDepth.size()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A indentação das tarefas do modelo não pode pular níveis.");
            }
            Long parentId = depth == 0 ? null : taskIdsByDepth.get(depth - 1);
            if (rawLine.trim().length() > 180) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cada tarefa inicial do modelo deve ter até 180 caracteres.");
            }
            TaskResponse task = taskService.create(new CreateTaskRequest(
                    rawLine.trim(), null, TaskStatus.TODO, TaskPriority.MEDIUM, null,
                    project.id(), null, parentId, TaskRecurrence.NONE, null), user);
            if (taskIdsByDepth.size() == depth) taskIdsByDepth.add(task.id());
            else taskIdsByDepth.set(depth, task.id());
        }
        return project;
    }

    private int indentation(String line) {
        int depth = 0;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == ' ') depth++;
            else if (ch == '\t') depth += 2;
            else break;
        }
        return depth / 2;
    }

    private void ensureCanManageTemplates(User user) {
        if (isMaster(user) || user.getRole() == Role.PO) return;
        throw new org.springframework.security.access.AccessDeniedException(
                "Somente PO e Master podem criar modelos.");
    }

    private boolean isMaster(User user) {
        return user.getRole() == Role.MASTER || user.getRole() == Role.ADMIN;
    }

    private TaskTemplate findTaskTemplate(Long id, User user) {
        return (isMaster(user) ? taskTemplates.findById(id) : taskTemplates.findByIdAndOwnerId(id, user.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Modelo de tarefa não encontrado."));
    }

    private ProjectTemplate findProjectTemplate(Long id, User user) {
        return (isMaster(user) ? projectTemplates.findById(id) : projectTemplates.findByIdAndOwnerId(id, user.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Modelo de projeto não encontrado."));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
