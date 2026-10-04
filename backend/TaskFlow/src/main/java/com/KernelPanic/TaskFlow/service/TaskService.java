package com.KernelPanic.TaskFlow.service;

import com.KernelPanic.TaskFlow.dto.CreateTaskRequest;
import com.KernelPanic.TaskFlow.dto.CreateTaskWorkLogRequest;
import com.KernelPanic.TaskFlow.dto.TaskWorkLogResponse;
import com.KernelPanic.TaskFlow.dto.CreateChecklistItemRequest;
import com.KernelPanic.TaskFlow.dto.UpdateChecklistItemRequest;
import com.KernelPanic.TaskFlow.dto.TaskChecklistItemResponse;
import com.KernelPanic.TaskFlow.dto.TaskResponse;
import com.KernelPanic.TaskFlow.dto.UpdateTaskRequest;
import com.KernelPanic.TaskFlow.entity.Project;
import com.KernelPanic.TaskFlow.entity.Task;
import com.KernelPanic.TaskFlow.entity.TaskChecklistItem;
import com.KernelPanic.TaskFlow.entity.TaskWorkLog;
import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.enums.TaskPriority;
import com.KernelPanic.TaskFlow.enums.TaskStatus;
import com.KernelPanic.TaskFlow.enums.TaskRecurrence;
import com.KernelPanic.TaskFlow.exception.TaskNotFoundException;
import com.KernelPanic.TaskFlow.repository.TaskRepository;
import com.KernelPanic.TaskFlow.repository.TaskChecklistItemRepository;
import com.KernelPanic.TaskFlow.repository.TaskWorkLogRepository;
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
    private final TaskWorkLogRepository workLogRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;
    private final NotificationService notificationService;

    @Transactional
    public TaskResponse create(CreateTaskRequest request, User currentUser) {
        Project project = projectService.findEntity(request.projectId());
        if (!isMaster(currentUser) && (currentUser.getRole() != com.KernelPanic.TaskFlow.enums.Role.PO
            || !project.getOwner().getId().equals(currentUser.getId()))) {
            throw new org.springframework.security.access.AccessDeniedException("PO só pode criar tarefas nos próprios projetos.");
        }
        Task parentTask = request.parentTaskId() == null ? null : findEntity(request.parentTaskId());
        if (parentTask != null) {
            ensureCanManage(parentTask, currentUser);
            if (!parentTask.getProject().getId().equals(project.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A subtarefa deve pertencer ao mesmo projeto da tarefa principal.");
            }
        }

        if (request.dueDate() != null && request.dueDate().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O prazo da tarefa deve ser hoje ou uma data futura.");
        }
        if (request.recurrenceEndDate() != null && request.recurrenceEndDate().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data final da repetição deve ser hoje ou uma data futura.");
        }
        User assignee = findAssignee(request.assigneeId());
        if (assignee != null && !projectService.isMember(project, assignee.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O responsável precisa fazer parte do projeto.");
        }
        if (currentUser.getRole() == com.KernelPanic.TaskFlow.enums.Role.DEV
            || currentUser.getRole() == com.KernelPanic.TaskFlow.enums.Role.USER) {
            throw new org.springframework.security.access.AccessDeniedException("Dev trabalha nas tarefas criadas e atribuídas pelo PO.");
        }
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

        return TaskResponse.fromEntity(taskRepository.saveAndFlush(task));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> list(Long projectId, User currentUser) {
        Project project = projectService.findEntity(projectId);
        if (isMaster(currentUser) || (currentUser.getRole() == com.KernelPanic.TaskFlow.enums.Role.PO
            && project.getOwner().getId().equals(currentUser.getId()))) {
            return taskRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream().map(TaskResponse::fromEntity).toList();
        }
        if (currentUser.getRole() == com.KernelPanic.TaskFlow.enums.Role.DEV
            || currentUser.getRole() == com.KernelPanic.TaskFlow.enums.Role.USER
            || currentUser.getRole() == com.KernelPanic.TaskFlow.enums.Role.PO) {
            return taskRepository.findByProjectIdAndAssigneeIdOrderByCreatedAtDesc(projectId, currentUser.getId())
                    .stream().map(TaskResponse::fromEntity).toList();
        }
        throw new org.springframework.security.access.AccessDeniedException("Você não tem acesso a este projeto.");
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> listByStatus(Long projectId, TaskStatus status, User currentUser) {
        return list(projectId, currentUser).stream().filter(task -> task.status().equals(status.name())).toList();
    }

    @Transactional(readOnly = true)
    public Task findEntity(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long id, User currentUser) {
        Task task = findEntity(id);
        boolean readable = isMaster(currentUser)
                || (currentUser.getRole() == com.KernelPanic.TaskFlow.enums.Role.PO
                    && task.getProject().getOwner().getId().equals(currentUser.getId()))
                || (task.getAssignee() != null && task.getAssignee().getId().equals(currentUser.getId()));
        if (!readable) throw new org.springframework.security.access.AccessDeniedException("Você não tem acesso a esta tarefa.");
        return TaskResponse.fromEntity(task);
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long id) {
        return TaskResponse.fromEntity(findEntity(id));
    }

    @Transactional
    public TaskResponse update(Long id, UpdateTaskRequest request, User currentUser) {
        Task task = findEntity(id);
        ensureCanManage(task, currentUser);
        if (!Objects.equals(task.getVersion(), request.version())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta tarefa foi atualizada em outro dispositivo. Atualize os dados antes de tentar novamente.");
        }
        LocalDate previousDueDate = task.getDueDate();
        TaskStatus previousStatus = task.getStatus();

        if ((currentUser.getRole() == com.KernelPanic.TaskFlow.enums.Role.DEV
            || currentUser.getRole() == com.KernelPanic.TaskFlow.enums.Role.USER)
            && (!Objects.equals(task.getTitle(), request.title().trim())
                || !Objects.equals(task.getDescription(), normalizeNullable(request.description()))
                || task.getPriority() != request.priority()
                || !Objects.equals(task.getDueDate(), request.dueDate())
                || !Objects.equals(task.getProject().getId(), request.projectId())
                || request.assigneeId() == null
                || !Objects.equals(task.getAssignee().getId(), request.assigneeId())
                || (request.recurrence() != null && task.getRecurrence() != request.recurrence())
                || !Objects.equals(task.getRecurrenceEndDate(), request.recurrenceEndDate()))) {
            throw new org.springframework.security.access.AccessDeniedException(
                "Dev só pode atualizar o status da tarefa atribuída a si.");
        }

        Project newProject = projectService.findEntity(request.projectId());
        if ((currentUser.getRole() == com.KernelPanic.TaskFlow.enums.Role.DEV
            || currentUser.getRole() == com.KernelPanic.TaskFlow.enums.Role.USER)
                && (!newProject.getId().equals(task.getProject().getId())
                || !currentUser.getId().equals(request.assigneeId()))) {
            throw new org.springframework.security.access.AccessDeniedException("Dev não pode mover ou reatribuir tarefas.");
        }
        if (!isMaster(currentUser) && currentUser.getRole() == com.KernelPanic.TaskFlow.enums.Role.PO
                && !newProject.getOwner().getId().equals(currentUser.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("PO só pode editar tarefas dos próprios projetos.");
        }
        if (!isMaster(currentUser) && task.getCreator().getId().equals(currentUser.getId())
                && currentUser.getRole() == com.KernelPanic.TaskFlow.enums.Role.DEV
                && task.getAssignee() != null && !task.getAssignee().getId().equals(currentUser.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Dev só pode editar tarefas atribuídas a si.");
        }
        if (task.getParentTask() != null && !task.getParentTask().getProject().getId().equals(newProject.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A subtarefa deve permanecer no projeto da tarefa principal.");
        }

        User assignee = findAssignee(request.assigneeId());
        if (assignee != null && !projectService.isMember(newProject, assignee.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O responsável precisa fazer parte do projeto.");
        }

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

        Task saved = taskRepository.saveAndFlush(task);
        if (saved.getStatus() != previousStatus) {
            notificationService.notifyProjectOwnerOfTaskStatus(saved, saved.getStatus());
        }
        return TaskResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<TaskWorkLogResponse> listWorkLogs(Long taskId, User currentUser) {
        get(taskId, currentUser);
        return workLogRepository.findByTaskIdOrderByCreatedAtDesc(taskId).stream()
                .map(TaskWorkLogResponse::fromEntity).toList();
    }

    @Transactional
    public TaskWorkLogResponse addWorkLog(Long taskId, CreateTaskWorkLogRequest request, User currentUser) {
        Task task = findEntity(taskId);
        ensureCanManage(task, currentUser);
        TaskWorkLog log = new TaskWorkLog();
        log.setTask(task);
        log.setUser(currentUser);
        log.setDurationMinutes(request.durationMinutes());
        log.setComment(request.comment() == null || request.comment().isBlank() ? null : request.comment().trim());
        return TaskWorkLogResponse.fromEntity(workLogRepository.save(log));
    }

    @Transactional
    public void delete(Long id, User currentUser) {
        Task task = findEntity(id);
        if (currentUser.getRole() == com.KernelPanic.TaskFlow.enums.Role.DEV) {
            throw new org.springframework.security.access.AccessDeniedException("Dev não pode excluir tarefas.");
        }
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
        if (isDeveloper(currentUser)) {
            throw new org.springframework.security.access.AccessDeniedException("Dev não pode alterar a estrutura do checklist.");
        }
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
        if (isDeveloper(currentUser) && (request.text() != null || request.done() == null)) {
            throw new org.springframework.security.access.AccessDeniedException("Dev só pode marcar itens do checklist como feitos ou pendentes.");
        }
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
        if (isDeveloper(currentUser)) {
            throw new org.springframework.security.access.AccessDeniedException("Dev não pode excluir itens do checklist.");
        }
        TaskChecklistItem item = checklistRepository.findByIdAndTaskId(itemId, taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item do checklist não encontrado."));
        checklistRepository.delete(item);
    }

    public void ensureCanManage(Task task, User currentUser) {
        boolean isMaster = isMaster(currentUser);
        boolean isDevAssigned = (currentUser.getRole() == com.KernelPanic.TaskFlow.enums.Role.DEV
            || currentUser.getRole() == com.KernelPanic.TaskFlow.enums.Role.USER)
                && task.getAssignee() != null && task.getAssignee().getId().equals(currentUser.getId());
        boolean isProjectOwner = task.getProject().getOwner().getId().equals(currentUser.getId());
        boolean isPoOwner = currentUser.getRole() == com.KernelPanic.TaskFlow.enums.Role.PO && isProjectOwner;

        if ((currentUser.getRole() == com.KernelPanic.TaskFlow.enums.Role.DEV
            || currentUser.getRole() == com.KernelPanic.TaskFlow.enums.Role.USER)
            && !isMaster && !isDevAssigned) {
            throw new org.springframework.security.access.AccessDeniedException("Dev só pode alterar tarefas atribuídas a si.");
        }
        if (!isMaster && !isDevAssigned && !isPoOwner) {
            throw new org.springframework.security.access.AccessDeniedException(
                "Somente o Master, o PO do projeto ou o Dev responsável pode realizar esta operação.");
        }
    }

    private boolean isMaster(User user) {
        return user.getRole() == com.KernelPanic.TaskFlow.enums.Role.MASTER || user.getRole() == com.KernelPanic.TaskFlow.enums.Role.ADMIN;
    }

    private boolean isDeveloper(User user) {
        return user.getRole() == com.KernelPanic.TaskFlow.enums.Role.DEV
                || user.getRole() == com.KernelPanic.TaskFlow.enums.Role.USER;
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
