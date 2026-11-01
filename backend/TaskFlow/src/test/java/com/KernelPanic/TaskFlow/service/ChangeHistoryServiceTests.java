package com.KernelPanic.TaskFlow.service;

import com.KernelPanic.TaskFlow.entity.ChangeHistory;
import com.KernelPanic.TaskFlow.entity.Project;
import com.KernelPanic.TaskFlow.entity.Task;
import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.repository.ChangeHistoryRepository;
import com.KernelPanic.TaskFlow.repository.ProjectRepository;
import com.KernelPanic.TaskFlow.repository.TaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChangeHistoryServiceTests {
    @Mock private ChangeHistoryRepository history;
    @Mock private ObjectMapper objectMapper;
    @Mock private ProjectRepository projects;
    @Mock private TaskRepository tasks;
    @InjectMocks private ChangeHistoryService service;

    @Test
    void recordsActorProjectAndAllowlistedDetails() throws Exception {
        User actor = User.builder().id(5L).name("Pessoa").build();
        User owner = User.builder().id(2L).name("PO").build();
        Project project = Project.builder().id(8L).owner(owner).build();
        Task task = Task.builder().id(11L).project(project).build();
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"status\":\"IN_PROGRESS\"}");

        service.record(project, task, actor, "TASK", 11L, "UPDATED", "alterou tarefa", Map.of("status", "IN_PROGRESS"));

        ArgumentCaptor<ChangeHistory> saved = ArgumentCaptor.forClass(ChangeHistory.class);
        verify(history).save(saved.capture());
        assertEquals(8L, saved.getValue().getProjectId());
        assertEquals(2L, saved.getValue().getProjectOwnerId());
        assertEquals(5L, saved.getValue().getActorId());
        assertEquals(11L, saved.getValue().getTaskId());
        assertEquals("{\"status\":\"IN_PROGRESS\"}", saved.getValue().getDetailsJson());
    }
}