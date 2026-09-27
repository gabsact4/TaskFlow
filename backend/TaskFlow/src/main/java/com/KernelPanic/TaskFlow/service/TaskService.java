package com.KernelPanic.TaskFlow.service;

import com.KernelPanic.TaskFlow.dto.CreateTaskRequest;
import com.KernelPanic.TaskFlow.dto.CreateChecklistItemRequest;
import com.KernelPanic.TaskFlow.dto.UpdateChecklistItemRequest;
import com.KernelPanic.TaskFlow.dto.TaskChecklistItemResponse;
import com.KernelPanic.TaskFlow.dto.TaskResponse;
import com.KernelPanic.TaskFlow.dto.UpdateTaskRequest;
import com.KernelPanic.TaskFlow.entity.Project;
import com.KernelPanic.TaskFlow.entity.Task;
import com.KernelPanic.TaskFlow.entity.TaskChecklistItem;
import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.enums.TaskPriority;
import com.KernelPanic.TaskFlow.enums.TaskStatus;
import com.KernelPanic.TaskFlow.enums.TaskRecurrence;
import com.KernelPanic.TaskFlow.exception.TaskNotFoundException;
import com.KernelPanic.TaskFlow.repository.TaskRepository;
import com.KernelPanic.TaskFlow.repository.TaskChecklistItemRepository;
import com.KernelPanic.TaskFlow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskChecklistItemRepository checklistRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;

    @Transactional
    public TaskResponse create(CreateTaskRequest request, User currentUser) {
        Project project = projectService.findEntity(request.projectId());
        Task parentTask = request.parentTaskId() == null ? null : findEntity(request.parentTaskId());
        if (parentTask != null) {
            ensureCanManage(parentTask, currentUser);
            if (!parentTask.getProject().getId().equals(project.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A subtarefa deve pertencer ao mesmo projeto da tarefa principal.");
            }
        }

        User assignee = findAssignee(request.assigneeId());
        TaskRecurrence recurrence = request.recurrence() == null ? TaskRecurrence.NONE : request.recurrence();
        LocalDate recurrenceEndDate = recurrence == TaskRecurrence.NONE ? null : request.recurrenceEndDate();
        LocalDate nextOccurrenceDate = recurrence == TaskRecurrence.NONE ? null
                : nextOccurrence(recurrence, request.dueDate());
        validateRecurrence(recurrence, recurrenceEndDate, nextOccurrenceDate);

        Task task = Task.builder()
                .title(request.title().trim())
                .description(normalizeNullable(request.description()))
                .status(request.status() == null ? TaskStatus.TODO : request.status())
                .priority(request.priority() == null ? TaskPriority.MEDIUM : request.priority())
                .dueDate(request.dueDate())
                .project(project)
                .assignee(assignee)
                .creator(currentUser)
                .parentTask(parentTask)
                .recurrence(recurrence)
                .recurrenceEndDate(recurrenceEndDate)
                .nextOccurrenceDate(nextOccurrenceDate)
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
        LocalDate previousDueDate = task.getDueDate();

        Project newProject = projectService.findEntity(request.projectId());
        if (task.getParentTask() != null && !task.getParentTask().getProject().getId().equals(newProject.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A subtarefa deve permanecer no projeto da tarefa principal.");
        }

        User assignee = findAssignee(request.assigneeId());

        task.setTitle(request.title().trim());
        task.setDescription(normalizeNullable(request.description()));
        task.setStatus(request.status());
        task.setPriority(request.priority());
        task.setDueDate(request.dueDate());
        task.setProject(newProject);
        task.setAssignee(assignee);
        if (request.recurrence() != null) {
            if (request.recurrence() == TaskRecurrence.NONE) {
                task.setRecurrence(TaskRecurrence.NONE);
                task.setRecurrenceEndDate(null);
                task.setNextOccurrenceDate(null);
            } else {
                boolean scheduleChanged = task.getRecurrence() != request.recurrence()
                        || !Objects.equals(previousDueDate, request.dueDate());
                task.setRecurrence(request.recurrence());
                task.setRecurrenceEndDate(request.recurrenceEndDate());
                LocalDate next = scheduleChanged
                        ? nextOccurrence(request.recurrence(), request.dueDate())
                        : task.getNextOccurrenceDate();
                validateRecurrence(request.recurrence(), request.recurrenceEndDate(), next);
                task.setNextOccurrenceDate(next);
            }
        } else if (task.getRecurrence() != TaskRecurrence.NONE && !Objects.equals(previousDueDate, request.dueDate())) {
            LocalDate next = nextOccurrence(task.getRecurrence(), request.dueDate());
            validateRecurrence(task.getRecurrence(), task.getRecurrenceEndDate(), next);
            task.setNextOccurrenceDate(next);
        }

        return TaskResponse.fromEntity(taskRepository.save(task));
    }

    @Transactional
    public void delete(Long id, User currentUser) {
        Task task = findEntity(id);
        ensureCanManage(task, currentUser);
        taskRepository.delete(task);
    }

    @Transactional
    public List<TaskChecklistItemResponse> listChecklist(Long taskId, User currentUser) {
        Task task = findEntity(taskId);
        ensureCanManage(task, currentUser);
        return task.getChecklistItems().stream().map(TaskChecklistItemResponse::fromEntity).toList();
    }

    @Transactional
    public TaskChecklistItemResponse addChecklistItem(Long taskId, CreateChecklistItemRequest request, User currentUser) {
        Task task = findEntity(taskId);
        ensureCanManage(task, currentUser);
        TaskChecklistItem item = TaskChecklistItem.builder()
                .task(task)
                .text(request.text().trim())
                .position(checklistRepository.countByTaskId(taskId))
                .build();
        TaskChecklistItem saved = checklistRepository.save(item);
        task.getChecklistItems().add(saved);
        return TaskChecklistItemResponse.fromEntity(saved);
    }

    @Transactional
    public TaskChecklistItemResponse updateChecklistItem(Long taskId, Long itemId, UpdateChecklistItemRequest request, User currentUser) {
        Task task = findEntity(taskId);
        ensureCanManage(task, currentUser);
        TaskChecklistItem item = checklistRepository.findByIdAndTaskId(itemId, taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item do checklist não encontrado."));
        if (request.text() != null) {
            if (request.text().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O texto do item não pode ficar vazio.");
            item.setText(request.text().trim());
        }
        if (request.done() != null) item.setDone(request.done());
        if (request.text() == null && request.done() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o texto ou o estado do item.");
        }
        return TaskChecklistItemResponse.fromEntity(checklistRepository.save(item));
    }

    @Transactional
    public void deleteChecklistItem(Long taskId, Long itemId, User currentUser) {
        Task task = findEntity(taskId);
        ensureCanManage(task, currentUser);
        TaskChecklistItem item = checklistRepository.findByIdAndTaskId(itemId, taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item do checklist não encontrado."));
        checklistRepository.delete(item);
    }

    public void ensureCanManage(Task task, User currentUser) {
        boolean isCreator = task.getCreator().getId().equals(currentUser.getId());
        boolean isProjectOwner = task.getProject().getOwner().getId().equals(currentUser.getId());

        if (!isCreator && !isProjectOwner) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Somente o criador da tarefa ou o proprietário do projeto pode realizar esta operação.");
        }
    }

    private void validateRecurrence(TaskRecurrence recurrence, LocalDate endDate, LocalDate nextOccurrenceDate) {
        if (recurrence != TaskRecurrence.NONE && endDate != null && nextOccurrenceDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data final da recorrência deve ser posterior à próxima ocorrência.");
        }
    }

    private LocalDate nextOccurrence(TaskRecurrence recurrence, LocalDate dueDate) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate baseline = dueDate != null && dueDate.isAfter(today) ? dueDate : today;
        return recurrence.next(baseline);
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
