package com.KernelPanic.TaskFlow.dto;
import java.time.LocalDate;
import java.util.List;
public record ProductivityReportResponse(LocalDate from, LocalDate to, double trackedHours, long workLogCount, long completedTasks, long completedFocusSessions, List<MemberReport> members, List<ProjectReport> projects) {
    public record MemberReport(Long userId, String userName, double trackedHours, long workLogCount, long completedTasks, long focusSessions) {}
    public record ProjectReport(Long projectId, String projectName, double trackedHours, long workLogCount, long completedTasks) {}
}