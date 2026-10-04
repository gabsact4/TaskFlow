package com.KernelPanic.TaskFlow.service;

import com.KernelPanic.TaskFlow.entity.Project;
import com.KernelPanic.TaskFlow.entity.Task;
import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.enums.Role;
import com.KernelPanic.TaskFlow.enums.TaskStatus;
import com.KernelPanic.TaskFlow.repository.NotificationRepository;
import com.KernelPanic.TaskFlow.repository.TaskRepository;
import com.KernelPanic.TaskFlow.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTests {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void createsCriticalAlertsForTomorrowAndOverdueTasks() {
        User recipient = user(1L, 2);
        when(userRepository.findByRoleIn(anyCollection())).thenReturn(List.of());
        when(taskRepository.findOpenTopLevelTasksWithDueDate(TaskStatus.DONE)).thenReturn(List.of(
                task(10L, recipient, LocalDate.now(ZoneOffset.UTC).plusDays(1)),
                task(11L, recipient, LocalDate.now(ZoneOffset.UTC).minusDays(1))));

        notificationService.generateDueDateNotifications();

        ArgumentCaptor<String> type = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> title = ArgumentCaptor.forClass(String.class);
        verify(notificationRepository, times(2)).insertIfAbsent(eq(1L), anyLong(), type.capture(), title.capture(),
                anyString(), anyString(), any());
        assertEquals(List.of("CRITICAL", "CRITICAL"), type.getAllValues());
        assertEquals("Prazo crítico: vence amanhã", title.getAllValues().getFirst());
        assertEquals("Prazo vencido", title.getAllValues().get(1));
    }

    @Test
    void createsPersonalReminderOnlyWithinRecipientPreference() {
        User recipient = user(2L, 5);
        when(userRepository.findByRoleIn(anyCollection())).thenReturn(List.of());
        when(taskRepository.findOpenTopLevelTasksWithDueDate(TaskStatus.DONE)).thenReturn(List.of(
                task(20L, recipient, LocalDate.now(ZoneOffset.UTC).plusDays(5)),
                task(21L, recipient, LocalDate.now(ZoneOffset.UTC).plusDays(6))));

        notificationService.generateDueDateNotifications();

        ArgumentCaptor<String> type = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> eventKey = ArgumentCaptor.forClass(String.class);
        verify(notificationRepository).insertIfAbsent(eq(2L), eq(20L), type.capture(), anyString(), anyString(),
                eventKey.capture(), any());
        assertEquals("REMINDER", type.getValue());
        assertEquals("task:20:user:2:reminder:" + LocalDate.now(ZoneOffset.UTC).plusDays(5), eventKey.getValue());
    }

    private User user(Long id, int reminderDays) {
        return User.builder().id(id).role(Role.PO).reminderDays(reminderDays).build();
    }

    private Task task(Long id, User recipient, LocalDate dueDate) {
        Project project = Project.builder().id(100L + id).name("Projeto").owner(recipient).build();
        return Task.builder().id(id).title("Tarefa " + id).status(TaskStatus.TODO)
                .dueDate(dueDate).project(project).assignee(recipient).creator(recipient).build();
    }
}
