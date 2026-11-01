package com.KernelPanic.TaskFlow.dto;
import java.time.LocalDate;
import java.util.List;
public record TeamCapacityResponse(LocalDate from, LocalDate to, List<MemberCapacity> members, List<TaskConflict> conflicts) {
    public record MemberCapacity(Long userId, String userName, double plannedHours, double availableHours, double utilizationPercent, int openTasks, int approvedAbsenceDays) {}
    public record TaskConflict(Long userId, String userName, Long firstTaskId, String firstTaskTitle, Long secondTaskId, String secondTaskTitle, LocalDate overlapStart, LocalDate overlapEnd) {}
}