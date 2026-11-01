package com.KernelPanic.TaskFlow.service;

import com.KernelPanic.TaskFlow.dto.FocusSessionRequest;
import com.KernelPanic.TaskFlow.dto.FocusSessionResponse;
import com.KernelPanic.TaskFlow.dto.FinishFocusSessionRequest;
import com.KernelPanic.TaskFlow.entity.FocusSession;
import com.KernelPanic.TaskFlow.entity.Task;
import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.repository.FocusSessionRepository;
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
public class FocusSessionService {
    private final FocusSessionRepository sessions;
    private final UserRepository users;
    private final TaskService taskService;
    private final ChangeHistoryService historyService;

    @Transactional(readOnly = true)
    public FocusSessionResponse active(User user) {
        return sessions.findFirstByUserIdAndEndedAtIsNullOrderByStartedAtDesc(user.getId()).map(FocusSessionResponse::fromEntity).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<FocusSessionResponse> history(User user) {
        Instant from = Instant.now().minus(Duration.ofDays(30));
        Instant to = Instant.now().plusSeconds(1);
        return sessions.findByUserIdAndCompletedTrueAndEndedAtGreaterThanEqualAndEndedAtLessThan(user.getId(), from, to)
                .stream().map(FocusSessionResponse::fromEntity).toList();
    }

    @Transactional
    public FocusSessionResponse start(FocusSessionRequest request, User user) {
        User lockedUser = users.findByIdForUpdate(user.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        if (sessions.findFirstByUserIdAndEndedAtIsNullOrderByStartedAtDesc(user.getId()).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Finalize a sessão de foco atual antes de iniciar outra.");
        Task task = request.taskId() == null ? null : taskService.findEntity(request.taskId());
        if (task != null) taskService.ensureCanManage(task, user);
        int minutes = request.plannedMinutes() == null ? 25 : request.plannedMinutes();
        FocusSession session = FocusSession.builder().user(lockedUser).task(task).startedAt(Instant.now()).plannedMinutes(minutes).completed(false).build();
        return FocusSessionResponse.fromEntity(sessions.saveAndFlush(session));
    }

    @Transactional
    public FocusSessionResponse finish(Long id, FinishFocusSessionRequest request, User user) {
        users.findByIdForUpdate(user.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        FocusSession session = sessions.findById(id).filter(row -> row.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sessão de foco não encontrada."));
        if (session.getEndedAt() != null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta sessão já foi encerrada.");
        Instant now = Instant.now();
        long seconds = Math.max(0, Duration.between(session.getStartedAt(), now).getSeconds());
        if (request.completed() && seconds + 3 < session.getPlannedMinutes() * 60L) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A sessão ainda não atingiu a duração configurada.");
        }
        int actualMinutes = (int) Math.max(0, Math.min(1440, (seconds + 59) / 60));
        if (request.completed() && actualMinutes < 1) actualMinutes = 1;
        session.setEndedAt(now);
        session.setActualMinutes(actualMinutes);
        session.setCompleted(request.completed());
        if (session.getTask() != null) historyService.record(session.getTask().getProject(), session.getTask(), user, "FOCUS_SESSION", session.getId(), request.completed() ? "COMPLETED" : "ABORTED",
            request.completed() ? "concluiu uma sessão Pomodoro em \"" + session.getTask().getTitle() + "\"" : "encerrou uma sessão de foco em \"" + session.getTask().getTitle() + "\"",
            java.util.Map.of("plannedMinutes", session.getPlannedMinutes(), "actualMinutes", session.getActualMinutes(), "completed", session.getCompleted()));
        return FocusSessionResponse.fromEntity(session);
    }
}