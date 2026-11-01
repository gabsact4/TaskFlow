package com.KernelPanic.TaskFlow.service;

import com.KernelPanic.TaskFlow.dto.StartTaskTimerRequest;
import com.KernelPanic.TaskFlow.dto.TaskTimerResponse;
import com.KernelPanic.TaskFlow.dto.TaskWorkLogResponse;
import com.KernelPanic.TaskFlow.entity.Task;
import com.KernelPanic.TaskFlow.entity.TaskTimerSession;
import com.KernelPanic.TaskFlow.entity.TaskWorkLog;
import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.enums.TaskStatus;
import com.KernelPanic.TaskFlow.repository.TaskTimerSessionRepository;
import com.KernelPanic.TaskFlow.repository.TaskWorkLogRepository;
import com.KernelPanic.TaskFlow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service @RequiredArgsConstructor
public class TaskTimerService {
    private static final List<TaskTimerSession.TimerStatus> ACTIVE = List.of(TaskTimerSession.TimerStatus.RUNNING, TaskTimerSession.TimerStatus.PAUSED);
    private final TaskTimerSessionRepository timers;
    private final TaskWorkLogRepository workLogs;
    private final TaskService taskService;
    private final UserRepository users;
    private final ChangeHistoryService historyService;

    @Transactional(readOnly = true)
    public TaskTimerResponse active(User user) {
        return timers.findFirstByUserIdAndStatusInOrderByStartedAtDesc(user.getId(), ACTIVE)
                .map(session -> TaskTimerResponse.fromEntity(session, elapsedSeconds(session, Instant.now()))).orElse(null);
    }

    @Transactional
    public TaskTimerResponse start(StartTaskTimerRequest request, User user) {
        User lockedUser = users.findByIdForUpdate(user.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        ensureNoActiveTimer(lockedUser);
        Task task = taskService.findEntity(request.taskId());
        taskService.ensureCanManage(task, user);
        if (task.getStatus() == TaskStatus.DONE) throw new ResponseStatusException(HttpStatus.CONFLICT, "Não é possível iniciar um timer em uma tarefa concluída.");
        TaskTimerSession session = TaskTimerSession.builder().task(task).user(lockedUser).status(TaskTimerSession.TimerStatus.RUNNING).startedAt(Instant.now()).pausedSeconds(0L).build();
        return toResponse(timers.saveAndFlush(session));
    }

    @Transactional
    public TaskTimerResponse pause(User user) {
        lockUser(user);
        TaskTimerSession session = requireActive(user);
        if (session.getStatus() != TaskTimerSession.TimerStatus.RUNNING) throw new ResponseStatusException(HttpStatus.CONFLICT, "O timer já está pausado.");
        session.setStatus(TaskTimerSession.TimerStatus.PAUSED);
        session.setPausedAt(Instant.now());
        return toResponse(session);
    }

    @Transactional
    public TaskTimerResponse resume(User user) {
        lockUser(user);
        TaskTimerSession session = requireActive(user);
        if (session.getStatus() != TaskTimerSession.TimerStatus.PAUSED) throw new ResponseStatusException(HttpStatus.CONFLICT, "O timer não está pausado.");
        Instant now = Instant.now();
        session.setPausedSeconds(session.getPausedSeconds() + Duration.between(session.getPausedAt(), now).getSeconds());
        session.setPausedAt(null);
        session.setStatus(TaskTimerSession.TimerStatus.RUNNING);
        return toResponse(session);
    }

    @Transactional
    public TaskWorkLogResponse stop(User user, String comment) {
        lockUser(user);
        TaskTimerSession session = requireActive(user);
        Instant now = Instant.now();
        long seconds = elapsedSeconds(session, now);
        if (seconds < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O timer precisa registrar ao menos um segundo.");
        session.setStatus(TaskTimerSession.TimerStatus.STOPPED);
        session.setEndedAt(now);
        session.setPausedAt(null);
        TaskWorkLog log = new TaskWorkLog();
        log.setTask(session.getTask());
        log.setUser(session.getUser());
        log.setDurationMinutes((int) Math.max(1, Math.min(1440, (seconds + 59) / 60)));
        log.setComment(comment == null || comment.isBlank() ? "Tempo registrado pelo cronômetro" : comment.trim());
        TaskWorkLog saved = workLogs.save(log);
        historyService.record(session.getTask().getProject(), session.getTask(), session.getUser(), "WORK_LOG", saved.getId(), "CREATED",
            "registrou " + saved.getDurationMinutes() + " minuto(s) pelo cronômetro em \"" + session.getTask().getTitle() + "\"",
            java.util.Map.of("durationMinutes", saved.getDurationMinutes(), "source", "timer", "comment", saved.getComment()));
        return TaskWorkLogResponse.fromEntity(saved);
    }

    private TaskTimerSession requireActive(User user) {
        return timers.findFirstByUserIdAndStatusInOrderByStartedAtDesc(user.getId(), ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Você não possui um timer ativo."));
    }
    private void lockUser(User user) {
        users.findByIdForUpdate(user.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
    }
    private void ensureNoActiveTimer(User user) {
        if (timers.findFirstByUserIdAndStatusInOrderByStartedAtDesc(user.getId(), ACTIVE).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Encerre ou pause seu timer ativo antes de iniciar outro.");
        }
    }
    private TaskTimerResponse toResponse(TaskTimerSession session) { return TaskTimerResponse.fromEntity(session, elapsedSeconds(session, Instant.now())); }
    private long elapsedSeconds(TaskTimerSession session, Instant now) {
        Instant end = session.getStatus() == TaskTimerSession.TimerStatus.PAUSED ? session.getPausedAt() : now;
        long elapsed = Duration.between(session.getStartedAt(), end).getSeconds() - session.getPausedSeconds();
        return Math.max(0, elapsed);
    }
}