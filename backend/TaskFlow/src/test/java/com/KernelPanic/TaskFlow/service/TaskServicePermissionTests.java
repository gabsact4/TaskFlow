package com.KernelPanic.TaskFlow.service;

import com.KernelPanic.TaskFlow.dto.UpdateTaskRequest;
import com.KernelPanic.TaskFlow.entity.Project;
import com.KernelPanic.TaskFlow.entity.Task;
import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.enums.Role;
import com.KernelPanic.TaskFlow.enums.TaskPriority;
import com.KernelPanic.TaskFlow.enums.TaskStatus;
import com.KernelPanic.TaskFlow.repository.TaskChecklistItemRepository;
import com.KernelPanic.TaskFlow.repository.TaskRepository;
import com.KernelPanic.TaskFlow.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServicePermissionTests {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskChecklistItemRepository checklistRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectService projectService;

    @InjectMocks
    private TaskService taskService;

    @Test
    void rejectsStaleTaskVersionWithConflict() {
        User po = user(1L, Role.PO);
        Task task = task(12L, 3L, po, user(2L, Role.DEV));
        when(taskRepository.findById(12L)).thenReturn(Optional.of(task));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> taskService.update(12L, request("Original", 2L), po));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(taskRepository, never()).saveAndFlush(task);
    }

    @Test
    void developerCannotChangePlanningFieldsOnAssignedTask() {
        User dev = user(2L, Role.DEV);
        Task task = task(12L, 3L, user(1L, Role.PO), dev);
        when(taskRepository.findById(12L)).thenReturn(Optional.of(task));

        assertThrows(AccessDeniedException.class,
                () -> taskService.update(12L, request("Changed title", 3L), dev));

        verify(projectService, never()).findEntity(1L);
        verify(taskRepository, never()).saveAndFlush(task);
    }

    @Test
    void assignedDeveloperCanUpdateTaskStatus() {
        User dev = user(2L, Role.DEV);
        Task task = task(12L, 3L, user(1L, Role.PO), dev);
        when(taskRepository.findById(12L)).thenReturn(Optional.of(task));
        when(projectService.findEntity(1L)).thenReturn(task.getProject());
        when(userRepository.findById(2L)).thenReturn(Optional.of(dev));
        when(taskRepository.saveAndFlush(task)).thenReturn(task);

        taskService.update(12L, request("Original", TaskStatus.IN_PROGRESS, 3L), dev);

        assertEquals(TaskStatus.IN_PROGRESS, task.getStatus());
        verify(taskRepository).saveAndFlush(task);
    }

    private UpdateTaskRequest request(String title, Long version) {
        return request(title, TaskStatus.TODO, version);
    }

    private UpdateTaskRequest request(String title, TaskStatus status, Long version) {
        return new UpdateTaskRequest(title, "Description", status, TaskPriority.MEDIUM,
                null, 1L, 2L, null, null, version);
    }

    private User user(Long id, Role role) {
        return User.builder().id(id).role(role).build();
    }

    private Task task(Long id, Long version, User owner, User assignee) {
        Project project = Project.builder().id(1L).owner(owner).build();
        return Task.builder().id(id).version(version).title("Original").description("Description")
                .status(TaskStatus.TODO).priority(TaskPriority.MEDIUM).project(project)
                .creator(owner).assignee(assignee).build();
    }
}
