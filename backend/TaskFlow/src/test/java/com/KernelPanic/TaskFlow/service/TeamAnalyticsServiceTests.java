package com.KernelPanic.TaskFlow.service;

import com.KernelPanic.TaskFlow.dto.ProjectResponse;
import com.KernelPanic.TaskFlow.entity.Project;
import com.KernelPanic.TaskFlow.entity.Task;
import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.enums.ProjectStatus;
import com.KernelPanic.TaskFlow.enums.Role;
import com.KernelPanic.TaskFlow.enums.TaskStatus;
import com.KernelPanic.TaskFlow.repository.TaskRepository;
import com.KernelPanic.TaskFlow.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamAnalyticsServiceTests {
    @Mock private TaskRepository tasks;
    @Mock private UserRepository users;
    @Mock private ProjectService projectService;
    @Mock private UserAbsenceService absenceService;
    @InjectMocks private TeamAnalyticsService service;

    @Test
    void reportsEstimatedLoadAndOverlappingTasksForProjectTeam() {
        User master = User.builder().id(1L).name("Master").role(Role.MASTER).build();
        User developer = User.builder().id(2L).name("Dev").role(Role.DEV).build();
        Project project = Project.builder().id(10L).owner(master).members(Set.of(developer)).status(ProjectStatus.ACTIVE).build();
        ProjectResponse summary = new ProjectResponse(10L, "Projeto", "TF", null, "ACTIVE", 1L, "Master", null, null, List.of(1L, 2L));
        LocalDate monday = LocalDate.now();
        while (monday.getDayOfWeek() != DayOfWeek.MONDAY) monday = monday.plusDays(1);
        LocalDate tuesday = monday.plusDays(1);
        Task first = task(100L, "Planejamento A", project, developer, monday, tuesday);
        Task second = task(101L, "Planejamento B", project, developer, monday, tuesday);
        when(projectService.list(master)).thenReturn(List.of(summary));
        when(projectService.findEntity(10L)).thenReturn(project);
        when(tasks.findByProjectIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(first, second));
        when(absenceService.approvedOverlapping(monday, monday.plusDays(4))).thenReturn(List.of());

        var result = service.capacity(monday, monday.plusDays(4), master);

        var devCapacity = result.members().stream().filter(member -> member.userId().equals(2L)).findFirst().orElseThrow();
        assertEquals(16.0, devCapacity.plannedHours());
        assertEquals(40.0, devCapacity.availableHours());
        assertEquals(1, result.conflicts().size());
        assertEquals(monday, result.conflicts().getFirst().overlapStart());
    }

    private Task task(Long id, String title, Project project, User assignee, LocalDate start, LocalDate end) {
        return Task.builder().id(id).title(title).project(project).assignee(assignee).status(TaskStatus.TODO)
                .plannedStartDate(start).dueDate(end).estimatedHours(BigDecimal.valueOf(8)).build();
    }
}