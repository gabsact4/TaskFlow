package com.KernelPanic.TaskFlow.service;

import com.KernelPanic.TaskFlow.dto.TeamCapacityResponse;
import com.KernelPanic.TaskFlow.entity.Project;
import com.KernelPanic.TaskFlow.entity.Task;
import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.entity.UserAbsence;
import com.KernelPanic.TaskFlow.enums.Role;
import com.KernelPanic.TaskFlow.enums.TaskStatus;
import com.KernelPanic.TaskFlow.repository.TaskRepository;
import com.KernelPanic.TaskFlow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class TeamAnalyticsService {
    private static final BigDecimal DAILY_CAPACITY = BigDecimal.valueOf(8);
    private final TaskRepository tasks;
    private final UserRepository users;
    private final ProjectService projectService;
    private final UserAbsenceService absenceService;

    @Transactional(readOnly = true)
    public TeamCapacityResponse capacity(LocalDate from, LocalDate to, User current) {
        if (from == null || to == null || from.isAfter(to)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um intervalo de datas válido.");
        if (to.isAfter(from.plusDays(365))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O intervalo máximo da análise é de 366 dias.");
        if (current.getRole() == Role.USER || current.getRole() == Role.DEV) return capacityForSelf(from, to, current);
        List<Project> visibleProjects = projectService.list(current).stream()
                .map(p -> projectService.findEntity(p.id())).toList();
        Set<Long> projectIds = visibleProjects.stream().map(project -> project.getId()).collect(Collectors.toSet());
        List<Task> scopedTasks = projectIds.stream().flatMap(id -> tasks.findByProjectIdOrderByCreatedAtDesc(id).stream())
                .filter(t -> t.getParentTask() == null && t.getStatus() != TaskStatus.DONE).toList();
        LinkedHashMap<Long, User> team = new LinkedHashMap<>();
        for (Project project : visibleProjects) {
            team.put(project.getOwner().getId(), project.getOwner());
            project.getMembers().forEach(member -> team.put(member.getId(), member));
        }
        scopedTasks.stream().map(task -> task.getAssignee()).filter(Objects::nonNull).forEach(member -> team.put(member.getId(), member));
        return calculate(from, to, new ArrayList<>(team.values()), scopedTasks);
    }

    private TeamCapacityResponse capacityForSelf(LocalDate from, LocalDate to, User current) {
        List<Project> projects = projectService.list(current).stream().map(p -> projectService.findEntity(p.id())).toList();
        Set<Long> ids = projects.stream().map(project -> project.getId()).collect(Collectors.toSet());
        List<Task> ownTasks = ids.stream().flatMap(id -> tasks.findByProjectIdAndAssigneeIdOrderByCreatedAtDesc(id, current.getId()).stream())
                .filter(t -> t.getParentTask() == null && t.getStatus() != TaskStatus.DONE).toList();
        return calculate(from, to, List.of(users.findById(current.getId()).orElse(current)), ownTasks);
    }

    private TeamCapacityResponse calculate(LocalDate from, LocalDate to, List<User> team, List<Task> openTasks) {
        List<UserAbsence> approved = absenceService.approvedOverlapping(from, to);
        Map<Long, List<UserAbsence>> absencesByUser = approved.stream().collect(Collectors.groupingBy(a -> a.getUser().getId()));
        Map<Long, List<Task>> tasksByUser = openTasks.stream().filter(t -> t.getAssignee() != null).collect(Collectors.groupingBy(t -> t.getAssignee().getId()));
        List<TeamCapacityResponse.MemberCapacity> members = new ArrayList<>();
        List<TeamCapacityResponse.TaskConflict> conflicts = new ArrayList<>();
        for (User member : team) {
            List<Task> assigned = tasksByUser.getOrDefault(member.getId(), List.of());
            List<UserAbsence> memberAbsences = absencesByUser.getOrDefault(member.getId(), List.of());
            int workingDays = weekdays(from, to);
            int absenceDays = countAbsenceWeekdays(memberAbsences, from, to);
            BigDecimal available = DAILY_CAPACITY.multiply(BigDecimal.valueOf(Math.max(0, workingDays - absenceDays)));
            BigDecimal planned = assigned.stream().map(task -> allocatedHours(task, from, to)).reduce(BigDecimal.ZERO, (left, right) -> left.add(right));
            double utilization = available.signum() == 0 ? (planned.signum() > 0 ? 100 : 0)
                    : planned.multiply(BigDecimal.valueOf(100)).divide(available, 2, java.math.RoundingMode.HALF_UP).doubleValue();
            members.add(new TeamCapacityResponse.MemberCapacity(member.getId(), member.getName(), planned.doubleValue(), available.doubleValue(), utilization, assigned.size(), absenceDays));
            findConflicts(member, assigned, from, to, conflicts);
        }
        members.sort((left, right) -> Double.compare(right.utilizationPercent(), left.utilizationPercent()));
        return new TeamCapacityResponse(from, to, members, conflicts);
    }

    private BigDecimal allocatedHours(Task task, LocalDate from, LocalDate to) {
        BigDecimal estimate = task.getEstimatedHours();
        if (estimate == null || estimate.signum() <= 0) return BigDecimal.ZERO;
        LocalDate start = task.getPlannedStartDate() != null ? task.getPlannedStartDate() : task.getDueDate();
        LocalDate end = task.getDueDate() != null ? task.getDueDate() : start;
        if (start == null || end == null || end.isBefore(from) || start.isAfter(to)) return BigDecimal.ZERO;
        LocalDate clippedStart = start.isBefore(from) ? from : start;
        LocalDate clippedEnd = end.isAfter(to) ? to : end;
        int totalWorkdays = weekdays(start, end);
        int visibleWorkdays = weekdays(clippedStart, clippedEnd);
        if (totalWorkdays == 0 || visibleWorkdays == 0) return BigDecimal.ZERO;
        return estimate.multiply(BigDecimal.valueOf(visibleWorkdays)).divide(BigDecimal.valueOf(totalWorkdays), 2, java.math.RoundingMode.HALF_UP);
    }

    private void findConflicts(User member, List<Task> assigned, LocalDate from, LocalDate to, List<TeamCapacityResponse.TaskConflict> conflicts) {
        List<Task> scheduled = assigned.stream().filter(t -> t.getDueDate() != null || t.getPlannedStartDate() != null).toList();
        for (int i = 0; i < scheduled.size(); i++) for (int j = i + 1; j < scheduled.size(); j++) {
            Task a = scheduled.get(i), b = scheduled.get(j);
            LocalDate aStart = a.getPlannedStartDate() == null ? a.getDueDate() : a.getPlannedStartDate();
            LocalDate aEnd = a.getDueDate() == null ? aStart : a.getDueDate();
            LocalDate bStart = b.getPlannedStartDate() == null ? b.getDueDate() : b.getPlannedStartDate();
            LocalDate bEnd = b.getDueDate() == null ? bStart : b.getDueDate();
            LocalDate overlapStart = aStart.isAfter(bStart) ? aStart : bStart;
            LocalDate overlapEnd = aEnd.isBefore(bEnd) ? aEnd : bEnd;
            if (overlapStart.isBefore(from)) overlapStart = from;
            if (overlapEnd.isAfter(to)) overlapEnd = to;
            while (!overlapStart.isAfter(overlapEnd) && !isWeekday(overlapStart)) overlapStart = overlapStart.plusDays(1);
            while (!overlapEnd.isBefore(overlapStart) && !isWeekday(overlapEnd)) overlapEnd = overlapEnd.minusDays(1);
            if (!overlapStart.isAfter(overlapEnd)) conflicts.add(new TeamCapacityResponse.TaskConflict(member.getId(), member.getName(), a.getId(), a.getTitle(), b.getId(), b.getTitle(), overlapStart, overlapEnd));
        }
    }

    private int weekdays(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) return 0;
        int count = 0;
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) if (isWeekday(day)) count++;
        return count;
    }
    private int countAbsenceWeekdays(List<UserAbsence> absences, LocalDate from, LocalDate to) {
        Set<LocalDate> unavailable = new HashSet<>();
        for (UserAbsence absence : absences) {
            LocalDate start = absence.getStartDate().isBefore(from) ? from : absence.getStartDate();
            LocalDate end = absence.getEndDate().isAfter(to) ? to : absence.getEndDate();
            for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) if (isWeekday(day)) unavailable.add(day);
        }
        return unavailable.size();
    }
    private boolean isWeekday(LocalDate date) { return date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY; }
}