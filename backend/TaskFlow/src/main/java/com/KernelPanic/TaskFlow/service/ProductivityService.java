package com.KernelPanic.TaskFlow.service;

import com.KernelPanic.TaskFlow.dto.ProductivityGoalRequest;
import com.KernelPanic.TaskFlow.dto.ProductivityGoalResponse;
import com.KernelPanic.TaskFlow.dto.ProductivityReportResponse;
import com.KernelPanic.TaskFlow.entity.*;
import com.KernelPanic.TaskFlow.enums.Role;
import com.KernelPanic.TaskFlow.repository.FocusSessionRepository;
import com.KernelPanic.TaskFlow.repository.ProductivityGoalRepository;
import com.KernelPanic.TaskFlow.repository.TaskRepository;
import com.KernelPanic.TaskFlow.repository.TaskWorkLogRepository;
import com.KernelPanic.TaskFlow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class ProductivityService {
    private final ProductivityGoalRepository goals;
    private final TaskWorkLogRepository workLogs;
    private final TaskRepository tasks;
    private final FocusSessionRepository focusSessions;
    private final ProjectService projectService;
    private final UserRepository users;

    @Transactional(readOnly = true)
    public List<ProductivityGoalResponse> goals(User user) {
        return goals.findByUserIdOrderByPeriodTypeAscMetricAsc(user.getId()).stream().map(goal -> {
            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            LocalDate start = periodStart(today, goal.getPeriodType());
            LocalDate end = periodEnd(start, goal.getPeriodType());
            return ProductivityGoalResponse.fromEntity(goal, currentValue(user, goal.getMetric(), start, end), start, end);
        }).toList();
    }

    @Transactional
    public ProductivityGoalResponse saveGoal(ProductivityGoalRequest request, User user) {
        User goalOwner = users.findByIdForUpdate(user.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        ProductivityGoal goal = goals.findByUserIdAndMetricAndPeriodType(user.getId(), request.metric(), request.periodType())
            .orElseGet(() -> ProductivityGoal.builder().user(goalOwner).metric(request.metric()).periodType(request.periodType()).build());
        goal.setTargetValue(request.targetValue());
        ProductivityGoal saved = goals.save(goal);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate start = periodStart(today, saved.getPeriodType());
        LocalDate end = periodEnd(start, saved.getPeriodType());
        return ProductivityGoalResponse.fromEntity(saved, currentValue(user, saved.getMetric(), start, end), start, end);
    }

    @Transactional
    public void deleteGoal(Long id, User user) {
        ProductivityGoal goal = goals.findById(id).filter(row -> row.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meta não encontrada."));
        goals.delete(goal);
    }

    @Transactional(readOnly = true)
    public ProductivityReportResponse report(LocalDate requestedFrom, LocalDate requestedTo, Long requestedUserId, Long projectId, User current) {
        LocalDate to = requestedTo == null ? LocalDate.now(ZoneOffset.UTC) : requestedTo;
        LocalDate from = requestedFrom == null ? to.minusDays(6) : requestedFrom;
        if (from.isAfter(to)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data inicial do relatório deve ser anterior à final.");
        if (to.isAfter(from.plusDays(365))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O período máximo do relatório é de 366 dias.");

        List<Project> visibleProjects;
        if (projectId != null) {
            projectService.get(projectId, current);
            visibleProjects = List.of(projectService.findEntity(projectId));
        } else {
            visibleProjects = projectService.list(current).stream().map(row -> projectService.findEntity(row.id())).toList();
        }
        Set<Long> projectIds = visibleProjects.stream().map(project -> project.getId()).collect(Collectors.toSet());
        List<Task> scopedTasks = projectIds.stream().flatMap(id -> tasks.findByProjectIdOrderByCreatedAtDesc(id).stream()).toList();
        Map<Long, User> team = teamFor(visibleProjects, scopedTasks);
        Long selectedUser = requestedUserId;
        if (current.getRole() == Role.DEV || current.getRole() == Role.USER) {
            if (selectedUser != null && !selectedUser.equals(current.getId())) throw new AccessDeniedException("Você só pode consultar seu próprio relatório.");
            selectedUser = current.getId();
        } else if (selectedUser != null && !team.containsKey(selectedUser)) {
            throw new AccessDeniedException("O usuário selecionado não pertence à equipe visível.");
        }
        final Long reportUserId = selectedUser;

        Instant startInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endExclusive = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        List<Long> projectIdList = new ArrayList<>(projectIds);
        List<TaskWorkLog> selectedLogs = projectIdList.isEmpty() ? List.of() : workLogs.findForProjectsInPeriod(projectIdList, startInstant, endExclusive)
                .stream().filter(log -> reportUserId == null || log.getUser().getId().equals(reportUserId)).toList();
        List<Task> completed = scopedTasks.stream().filter(task -> task.getCompletedAt() != null
                && !task.getCompletedAt().isBefore(startInstant) && task.getCompletedAt().isBefore(endExclusive)
                && (reportUserId == null || (task.getAssignee() != null ? task.getAssignee().getId() : task.getCreator().getId()).equals(reportUserId))).toList();

        Set<Long> reportUsers = new LinkedHashSet<>();
        if (reportUserId != null) reportUsers.add(reportUserId);
        else reportUsers.addAll(team.keySet());
        List<Long> reportUserList = new ArrayList<>(reportUsers);
        List<FocusSession> focus = reportUserList.isEmpty() ? List.of() : focusSessions.findCompletedForUsersInPeriod(reportUserList, startInstant, endExclusive)
                .stream().filter(session -> session.getTask() == null || projectIds.contains(session.getTask().getProject().getId())).toList();

        Map<Long, Long> logSecondsByUser = new HashMap<>();
        Map<Long, Long> logCountByUser = new HashMap<>();
        Map<Long, Long> completedByUser = new HashMap<>();
        Map<Long, Long> focusByUser = new HashMap<>();
        Map<Long, Long> logSecondsByProject = new HashMap<>();
        Map<Long, Long> logCountByProject = new HashMap<>();
        Map<Long, Long> completedByProject = new HashMap<>();
        selectedLogs.forEach(log -> {
            logSecondsByUser.merge(log.getUser().getId(), log.getDurationMinutes() * 60L, (left, right) -> left + right);
            logCountByUser.merge(log.getUser().getId(), 1L, (left, right) -> left + right);
            logSecondsByProject.merge(log.getTask().getProject().getId(), log.getDurationMinutes() * 60L, (left, right) -> left + right);
            logCountByProject.merge(log.getTask().getProject().getId(), 1L, (left, right) -> left + right);
        });
        completed.forEach(task -> {
            Long contributor = task.getAssignee() == null ? task.getCreator().getId() : task.getAssignee().getId();
            completedByUser.merge(contributor, 1L, (left, right) -> left + right);
            completedByProject.merge(task.getProject().getId(), 1L, (left, right) -> left + right);
        });
        focus.forEach(session -> focusByUser.merge(session.getUser().getId(), 1L, (left, right) -> left + right));

        List<ProductivityReportResponse.MemberReport> memberReports = reportUsers.stream().map(id -> {
            User person = team.get(id);
            String name = person == null ? current.getName() : person.getName();
            return new ProductivityReportResponse.MemberReport(id, name, logSecondsByUser.getOrDefault(id, 0L) / 3600.0,
                    logCountByUser.getOrDefault(id, 0L), completedByUser.getOrDefault(id, 0L), focusByUser.getOrDefault(id, 0L));
        }).sorted((left, right) -> Double.compare(right.trackedHours(), left.trackedHours())).toList();
        List<ProductivityReportResponse.ProjectReport> projectReports = visibleProjects.stream().map(project -> new ProductivityReportResponse.ProjectReport(
                project.getId(), project.getName(), logSecondsByProject.getOrDefault(project.getId(), 0L) / 3600.0,
                logCountByProject.getOrDefault(project.getId(), 0L), completedByProject.getOrDefault(project.getId(), 0L))).toList();
        long seconds = selectedLogs.stream().mapToLong(log -> log.getDurationMinutes() * 60L).sum();
        return new ProductivityReportResponse(from, to, seconds / 3600.0, selectedLogs.size(), completed.size(), focus.size(), memberReports, projectReports);
    }

    private BigDecimal currentValue(User user, ProductivityGoal.Metric metric, LocalDate from, LocalDate to) {
        Instant start = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return switch (metric) {
            case TRACKED_HOURS -> BigDecimal.valueOf(workLogs.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(user.getId(), start, end)
                    .stream().mapToLong(log -> log.getDurationMinutes()).sum()).divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);
            case FOCUS_SESSIONS -> BigDecimal.valueOf(focusSessions.findByUserIdAndCompletedTrueAndEndedAtGreaterThanEqualAndEndedAtLessThan(user.getId(), start, end).size());
            case COMPLETED_TASKS -> {
                List<Long> ids = projectService.list(user).stream().map(row -> row.id()).toList();
                long count = ids.stream().flatMap(id -> tasks.findByProjectIdOrderByCreatedAtDesc(id).stream())
                        .filter(task -> task.getAssignee() != null && task.getAssignee().getId().equals(user.getId())
                                && task.getCompletedAt() != null && !task.getCompletedAt().isBefore(start) && task.getCompletedAt().isBefore(end)).count();
                yield BigDecimal.valueOf(count);
            }
        };
    }

    private Map<Long, User> teamFor(List<Project> projects, List<Task> scopedTasks) {
        Map<Long, User> team = new LinkedHashMap<>();
        for (Project project : projects) {
            team.put(project.getOwner().getId(), project.getOwner());
            project.getMembers().forEach(member -> team.put(member.getId(), member));
        }
        scopedTasks.forEach(task -> {
            if (task.getAssignee() != null) team.put(task.getAssignee().getId(), task.getAssignee());
            if (task.getCreator() != null) team.put(task.getCreator().getId(), task.getCreator());
        });
        return team;
    }

    private LocalDate periodStart(LocalDate date, ProductivityGoal.PeriodType period) {
        return period == ProductivityGoal.PeriodType.WEEKLY ? date.with(DayOfWeek.MONDAY) : date.withDayOfMonth(1);
    }
    private LocalDate periodEnd(LocalDate start, ProductivityGoal.PeriodType period) {
        return period == ProductivityGoal.PeriodType.WEEKLY ? start.plusDays(6) : start.withDayOfMonth(start.lengthOfMonth());
    }
}