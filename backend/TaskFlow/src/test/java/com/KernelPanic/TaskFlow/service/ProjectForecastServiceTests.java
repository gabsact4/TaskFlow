package com.KernelPanic.TaskFlow.service;

import com.KernelPanic.TaskFlow.entity.Project;
import com.KernelPanic.TaskFlow.entity.Task;
import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.enums.Role;
import com.KernelPanic.TaskFlow.enums.TaskStatus;
import com.KernelPanic.TaskFlow.repository.TaskRepository;
import com.KernelPanic.TaskFlow.repository.TaskWorkLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectForecastServiceTests {
    @Mock private ProjectService projectService;
    @Mock private TaskRepository tasks;
    @Mock private TaskWorkLogRepository workLogs;
    @Mock private UserAbsenceService absenceService;
    @InjectMocks private ProjectForecastService service;

    @Test
    void deductsLoggedHoursAndExcludesWeekendsFromProjectForecast() {
        User owner = User.builder().id(1L).name("PO").role(Role.PO).build();
        User developer = User.builder().id(2L).name("Dev").role(Role.DEV).build();
        Project project = Project.builder().id(12L).name("TaskFlow").owner(owner).build();
        Task task = Task.builder().id(30L).project(project).assignee(developer).status(TaskStatus.IN_PROGRESS)
                .estimatedHours(BigDecimal.TEN).build();
        when(projectService.findEntity(12L)).thenReturn(project);
        when(tasks.findByProjectIdOrderByCreatedAtDesc(12L)).thenReturn(List.of(task));
        when(workLogs.sumMinutesByTaskIds(List.of(30L))).thenReturn(List.<Object[]>of(new Object[]{30L, 120L}));
        when(absenceService.approvedOverlapping(any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of());

        var result = service.forecast(12L, owner);

        assertEquals(new BigDecimal("8.00"), result.remainingEstimatedHours());
        assertEquals(BigDecimal.valueOf(8), result.dailyCapacityHours());
        assertEquals(1, result.openTasks());
        assertEquals(0, result.tasksWithoutEstimate());
        assertNotNull(result.forecastCompletionDate());
    }

    @Test
    void omitsCompletionDateWhenOpenTasksHaveNoEstimate() {
        User owner = User.builder().id(1L).name("PO").role(Role.PO).build();
        Project project = Project.builder().id(12L).name("TaskFlow").owner(owner).build();
        Task task = Task.builder().id(30L).project(project).status(TaskStatus.TODO).build();
        when(projectService.findEntity(12L)).thenReturn(project);
        when(tasks.findByProjectIdOrderByCreatedAtDesc(12L)).thenReturn(List.of(task));
        when(absenceService.approvedOverlapping(any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of());

        var result = service.forecast(12L, owner);

        assertNull(result.forecastCompletionDate());
        assertEquals(1, result.tasksWithoutEstimate());
        assertEquals("LOW", result.confidence());
    }
}