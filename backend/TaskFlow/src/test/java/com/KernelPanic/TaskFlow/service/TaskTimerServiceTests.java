package com.KernelPanic.TaskFlow.service;

import com.KernelPanic.TaskFlow.dto.StartTaskTimerRequest;
import com.KernelPanic.TaskFlow.entity.Task;
import com.KernelPanic.TaskFlow.entity.TaskTimerSession;
import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.enums.Role;
import com.KernelPanic.TaskFlow.enums.TaskStatus;
import com.KernelPanic.TaskFlow.repository.TaskTimerSessionRepository;
import com.KernelPanic.TaskFlow.repository.TaskWorkLogRepository;
import com.KernelPanic.TaskFlow.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskTimerServiceTests {
    @Mock private TaskTimerSessionRepository timers;
    @Mock private TaskWorkLogRepository workLogs;
    @Mock private TaskService taskService;
    @Mock private UserRepository users;
    @Mock private ChangeHistoryService historyService;
    @InjectMocks private TaskTimerService service;

    @Test
    void startsTimerOnlyWhenUserHasNoActiveSession() {
        User user = User.builder().id(1L).role(Role.DEV).build();
        Task task = Task.builder().id(9L).title("Implementar fluxo").status(TaskStatus.IN_PROGRESS).build();
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(timers.findFirstByUserIdAndStatusInOrderByStartedAtDesc(eq(1L), anyCollection())).thenReturn(Optional.empty());
        when(taskService.findEntity(9L)).thenReturn(task);
        when(timers.saveAndFlush(any(TaskTimerSession.class))).thenAnswer(invocation -> {
            TaskTimerSession session = invocation.getArgument(0); session.setId(3L); return session;
        });

        var response = service.start(new StartTaskTimerRequest(9L), user);

        assertEquals("RUNNING", response.status());
        assertEquals(9L, response.taskId());
        verify(taskService).ensureCanManage(task, user);
    }

    @Test
    void rejectsStartingSecondTimer() {
        User user = User.builder().id(1L).role(Role.DEV).build();
        TaskTimerSession running = TaskTimerSession.builder().status(TaskTimerSession.TimerStatus.RUNNING).build();
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(timers.findFirstByUserIdAndStatusInOrderByStartedAtDesc(eq(1L), anyCollection())).thenReturn(Optional.of(running));

        assertThrows(ResponseStatusException.class, () -> service.start(new StartTaskTimerRequest(9L), user));
        verify(taskService, never()).findEntity(anyLong());
    }

    @Test
    void loadsPausedTimerWithoutPauseTimestamp() {
        User user = User.builder().id(1L).role(Role.DEV).build();
        Task task = Task.builder().id(9L).title("Implementar fluxo").build();
        TaskTimerSession paused = TaskTimerSession.builder()
                .id(3L).user(user).task(task).status(TaskTimerSession.TimerStatus.PAUSED)
                .startedAt(Instant.now().minusSeconds(30)).pausedAt(null).pausedSeconds(0L).build();
        when(timers.findFirstByUserIdAndStatusInOrderByStartedAtDesc(eq(1L), anyCollection()))
                .thenReturn(Optional.of(paused));

        var response = service.active(user);

        assertEquals("PAUSED", response.status());
        assertTrue(response.elapsedSeconds() >= 30);
    }

    @Test
    void stopsTimerAndCreatesWorkLogAtLeastOneMinute() {
        User user = User.builder().id(1L).role(Role.DEV).build();
        Task task = Task.builder().id(9L).build();
        TaskTimerSession session = TaskTimerSession.builder().id(3L).user(user).task(task).status(TaskTimerSession.TimerStatus.RUNNING).startedAt(Instant.now().minusSeconds(10)).pausedSeconds(0L).build();
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(timers.findFirstByUserIdAndStatusInOrderByStartedAtDesc(eq(1L), anyCollection())).thenReturn(Optional.of(session));
        when(workLogs.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.stop(user, null);

        assertEquals(1, response.durationMinutes());
        assertEquals("Tempo registrado pelo cronômetro", response.comment());
        assertEquals(TaskTimerSession.TimerStatus.STOPPED, session.getStatus());
    }
}