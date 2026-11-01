package com.KernelPanic.TaskFlow.service;

import com.KernelPanic.TaskFlow.dto.ProjectForecastResponse;
import com.KernelPanic.TaskFlow.entity.Project;
import com.KernelPanic.TaskFlow.entity.Task;
import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.entity.UserAbsence;
import com.KernelPanic.TaskFlow.enums.TaskStatus;
import com.KernelPanic.TaskFlow.repository.TaskRepository;
import com.KernelPanic.TaskFlow.repository.TaskWorkLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

@Service @RequiredArgsConstructor
public class ProjectForecastService {
    private static final BigDecimal DAILY_HOURS_PER_MEMBER = BigDecimal.valueOf(8);
    private final ProjectService projectService;
    private final TaskRepository tasks;
    private final TaskWorkLogRepository workLogs;
    private final UserAbsenceService absenceService;

    @Transactional(readOnly = true)
    public ProjectForecastResponse forecast(Long projectId, User current) {
        projectService.get(projectId, current);
        Project project = projectService.findEntity(projectId);
        List<Task> projectTasks = tasks.findByProjectIdOrderByCreatedAtDesc(projectId);
        List<Task> openTasks = projectTasks.stream()
                .filter(task -> task.getParentTask() == null && task.getStatus() != TaskStatus.DONE).toList();
        List<Long> taskIds = projectTasks.stream().map(task -> task.getId()).toList();
        Map<Long, Long> rootByTask = new HashMap<>();
        for (Task task : projectTasks) {
            Task root = task;
            while (root.getParentTask() != null) root = root.getParentTask();
            rootByTask.put(task.getId(), root.getId());
        }
        Map<Long, Long> loggedMinutes = new HashMap<>();
        if (!taskIds.isEmpty()) workLogs.sumMinutesByTaskIds(taskIds).forEach(row -> loggedMinutes.merge(rootByTask.get((Long) row[0]), ((Number) row[1]).longValue(), (left, right) -> left + right));

        BigDecimal remaining = BigDecimal.ZERO;
        int missingEstimates = 0;
        for (Task task : openTasks) {
            if (task.getEstimatedHours() == null) { missingEstimates++; continue; }
            BigDecimal spent = BigDecimal.valueOf(loggedMinutes.getOrDefault(task.getId(), 0L)).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
            BigDecimal taskRemaining = task.getEstimatedHours().subtract(spent);
            if (taskRemaining.signum() > 0) remaining = remaining.add(taskRemaining);
        }

        LinkedHashMap<Long, User> contributors = new LinkedHashMap<>();
        openTasks.stream().map(task -> task.getAssignee()).filter(Objects::nonNull).forEach(user -> contributors.put(user.getId(), user));
        if (contributors.isEmpty()) {
            contributors.put(project.getOwner().getId(), project.getOwner());
            project.getMembers().forEach(member -> contributors.put(member.getId(), member));
        }
        if (contributors.isEmpty()) contributors.put(project.getOwner().getId(), project.getOwner());
        LocalDate today = LocalDate.now();
        LocalDate horizon = today.plusYears(5);
        Set<Long> contributorIds = contributors.keySet();
        List<UserAbsence> absences = absenceService.approvedOverlapping(today, horizon).stream()
                .filter(absence -> contributorIds.contains(absence.getUser().getId())).toList();
        Map<LocalDate, Set<Long>> unavailableByDay = absenceDays(absences, today, horizon);
        BigDecimal dailyCapacity = DAILY_HOURS_PER_MEMBER.multiply(BigDecimal.valueOf(contributors.size()));
        LocalDate completionDate = missingEstimates > 0 ? null : calculateFinishDate(today, horizon, remaining, contributors.size(), unavailableByDay);
        String confidence = missingEstimates > 0 ? "LOW" : loggedMinutes.isEmpty() ? "MEDIUM" : "HIGH";
        return new ProjectForecastResponse(projectId, project.getName(), remaining.setScale(2, RoundingMode.HALF_UP), dailyCapacity,
                completionDate, openTasks.size(), missingEstimates, confidence);
    }

    private LocalDate calculateFinishDate(LocalDate today, LocalDate horizon, BigDecimal remaining, int teamSize, Map<LocalDate, Set<Long>> unavailable) {
        if (remaining.signum() <= 0) return today;
        BigDecimal hoursLeft = remaining;
        for (LocalDate day = today; !day.isAfter(horizon); day = day.plusDays(1)) {
            if (!isWeekday(day)) continue;
            int absent = unavailable.getOrDefault(day, Set.of()).size();
            BigDecimal capacity = DAILY_HOURS_PER_MEMBER.multiply(BigDecimal.valueOf(Math.max(0, teamSize - absent)));
            if (capacity.signum() == 0) continue;
            if (hoursLeft.compareTo(capacity) <= 0) return day;
            hoursLeft = hoursLeft.subtract(capacity);
        }
        return null;
    }

    private Map<LocalDate, Set<Long>> absenceDays(List<UserAbsence> absences, LocalDate from, LocalDate to) {
        Map<LocalDate, Set<Long>> result = new HashMap<>();
        for (UserAbsence absence : absences) {
            LocalDate start = absence.getStartDate().isBefore(from) ? from : absence.getStartDate();
            LocalDate end = absence.getEndDate().isAfter(to) ? to : absence.getEndDate();
            for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) if (isWeekday(day)) result.computeIfAbsent(day, ignored -> new HashSet<>()).add(absence.getUser().getId());
        }
        return result;
    }
    private boolean isWeekday(LocalDate day) { return day.getDayOfWeek() != DayOfWeek.SATURDAY && day.getDayOfWeek() != DayOfWeek.SUNDAY; }
}