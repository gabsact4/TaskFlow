package com.KernelPanic.TaskFlow.service;

import com.KernelPanic.TaskFlow.dto.CreateTaskRequest;
import com.KernelPanic.TaskFlow.dto.TaskResponse;
import com.KernelPanic.TaskFlow.dto.UpdateTaskRequest;
import com.KernelPanic.TaskFlow.entity.Project;
import com.KernelPanic.TaskFlow.entity.Task;
import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.enums.TaskPriority;
import com.KernelPanic.TaskFlow.enums.TaskStatus;
import com.KernelPanic.TaskFlow.exception.TaskNotFoundException;
import com.KernelPanic.TaskFlow.exception.TaskTitleAlreadyExistsException;
import com.KernelPanic.TaskFlow.repository.TaskRepository;
import com.KernelPanic.TaskFlow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;

    @Transactional
    public TaskResponse create(CreateTaskRequest request, User currentUser) {
        Project project = projectService.findEntity(request.projectId());


        User assignee = findAssignee(request.assigneeId());

        Task task = Task.builder()
                .title(request.title().trim())
                .description(normalizeNullable(request.description()))
                .status(request.status() == null ? TaskStatus.TODO : request.status())
                .priority(request.priority() == null ? TaskPriority.MEDIUM : request.priority())
                .dueDate(request.dueDate())
                .project(project)
                .assignee(assignee)
                .creator(currentUser)
                .build();

        return TaskResponse.fromEntity(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> list(Long projectId) {
        projectService.findEntity(projectId);
        return taskRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(TaskResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> listByStatus(Long projectId, TaskStatus status) {
        projectService.findEntity(projectId);
        return taskRepository.findByProjectIdAndStatusOrderByCreatedAtDesc(projectId, status)
                .stream()
                .map(TaskResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public Task findEntity(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long id) {
        return TaskResponse.fromEntity(findEntity(id));
    }

    @Transactional
    public TaskResponse update(Long id, UpdateTaskRequest request, User currentUser) {
        Task task = findEntity(id);
        ensureCanManage(task, currentUser);

        Project newProject = projectService.findEntity(request.projectId());

        User assignee = findAssignee(request.assigneeId());

        task.setTitle(request.title().trim());
        task.setDescription(normalizeNullable(request.description()));
        task.setStatus(request.status());
        task.setPriority(request.priority());
        task.setDueDate(request.dueDate());
        task.setProject(newProject);
        task.setAssignee(assignee);

        return TaskResponse.fromEntity(taskRepository.save(task));
    }

    @Transactional
    public void delete(Long id, User currentUser) {
        Task task = findEntity(id);
        ensureCanManage(task, currentUser);
        taskRepository.delete(task);
    }

    private void ensureCanManage(Task task, User currentUser) {
        boolean isCreator = task.getCreator().getId().equals(currentUser.getId());
        boolean isProjectOwner = task.getProject().getOwner().getId().equals(currentUser.getId());

        if (!isCreator && !isProjectOwner) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Somente o criador da tarefa ou o proprietário do projeto pode realizar esta operação.");
        }
    }

    private User findAssignee(Long id) {
        if (id == null) {
            return null;
        }
        return userRepository.findById(id)
                .orElseThrow(() -> new com.KernelPanic.TaskFlow.exception.UserNotFoundException(id));
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
