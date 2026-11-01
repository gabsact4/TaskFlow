package com.KernelPanic.TaskFlow.service;

import com.KernelPanic.TaskFlow.dto.ProductivityGoalRequest;
import com.KernelPanic.TaskFlow.entity.ProductivityGoal;
import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.enums.Role;
import com.KernelPanic.TaskFlow.repository.FocusSessionRepository;
import com.KernelPanic.TaskFlow.repository.ProductivityGoalRepository;
import com.KernelPanic.TaskFlow.repository.TaskRepository;
import com.KernelPanic.TaskFlow.repository.TaskWorkLogRepository;
import com.KernelPanic.TaskFlow.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductivityServiceTests {
    @Mock private ProductivityGoalRepository goals;
    @Mock private TaskWorkLogRepository workLogs;
    @Mock private TaskRepository tasks;
    @Mock private FocusSessionRepository focusSessions;
    @Mock private ProjectService projectService;
    @Mock private UserRepository users;
    @InjectMocks private ProductivityService service;

    @Test
    void savesGoalForTheAuthenticatedUserAndReturnsCurrentPeriodProgress() {
        User user = User.builder().id(4L).name("Pessoa").role(Role.DEV).build();
        when(users.findByIdForUpdate(4L)).thenReturn(Optional.of(user));
        ProductivityGoal existing = ProductivityGoal.builder().id(8L).user(user).metric(ProductivityGoal.Metric.TRACKED_HOURS)
                .periodType(ProductivityGoal.PeriodType.WEEKLY).targetValue(BigDecimal.valueOf(4)).build();
        when(goals.findByUserIdAndMetricAndPeriodType(4L, ProductivityGoal.Metric.TRACKED_HOURS, ProductivityGoal.PeriodType.WEEKLY)).thenReturn(Optional.of(existing));
        when(goals.save(existing)).thenReturn(existing);
        when(workLogs.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(4L), any(), any())).thenReturn(java.util.List.of());

        var saved = service.saveGoal(new ProductivityGoalRequest(ProductivityGoal.Metric.TRACKED_HOURS, ProductivityGoal.PeriodType.WEEKLY, BigDecimal.valueOf(6)), user);

        assertEquals(BigDecimal.valueOf(6), saved.targetValue());
        assertEquals(BigDecimal.ZERO.setScale(2), saved.currentValue());
        assertTrue(saved.progressPercent() == 0);
        verify(goals).save(existing);
    }
}