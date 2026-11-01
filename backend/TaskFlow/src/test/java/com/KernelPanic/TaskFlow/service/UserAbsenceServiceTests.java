package com.KernelPanic.TaskFlow.service;

import com.KernelPanic.TaskFlow.dto.CreateAbsenceRequest;
import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.entity.UserAbsence;
import com.KernelPanic.TaskFlow.enums.Role;
import com.KernelPanic.TaskFlow.repository.ProjectRepository;
import com.KernelPanic.TaskFlow.repository.TaskRepository;
import com.KernelPanic.TaskFlow.repository.UserAbsenceRepository;
import com.KernelPanic.TaskFlow.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAbsenceServiceTests {
    @Mock private UserAbsenceRepository absences;
    @Mock private UserRepository users;
    @Mock private ProjectRepository projects;
    @Mock private TaskRepository tasks;
    @InjectMocks private UserAbsenceService service;

    @Test
    void createsPendingAbsenceWhenDatesAreValidAndNotOverlapping() {
        User user = User.builder().id(7L).role(Role.DEV).build();
        when(users.findByIdForUpdate(7L)).thenReturn(java.util.Optional.of(user));
        LocalDate start = LocalDate.now().plusDays(5);
        when(absences.existsByUserIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(eq(7L), anyList(), eq(start.plusDays(2)), eq(start))).thenReturn(false);
        when(absences.save(any(UserAbsence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.request(new CreateAbsenceRequest(start, start.plusDays(2), " Férias "), user);

        assertEquals("PENDING", response.status());
        assertEquals("Férias", response.reason());
        assertEquals(start, response.startDate());
        verify(absences).save(any(UserAbsence.class));
    }

    @Test
    void rejectsAbsenceThatOverlapsPendingOrApprovedPeriod() {
        User user = User.builder().id(7L).role(Role.DEV).build();
        when(users.findByIdForUpdate(7L)).thenReturn(java.util.Optional.of(user));
        LocalDate start = LocalDate.now().plusDays(5);
        when(absences.existsByUserIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(eq(7L), anyList(), eq(start.plusDays(1)), eq(start))).thenReturn(true);

        assertThrows(ResponseStatusException.class,
                () -> service.request(new CreateAbsenceRequest(start, start.plusDays(1), null), user));
        verify(absences, never()).save(any(UserAbsence.class));
    }

    @Test
    void rejectsReversedDateRange() {
        User user = User.builder().id(7L).role(Role.DEV).build();
        assertThrows(ResponseStatusException.class,
                () -> service.request(new CreateAbsenceRequest(LocalDate.now().plusDays(3), LocalDate.now().plusDays(2), null), user));
        verifyNoInteractions(absences);
    }
}