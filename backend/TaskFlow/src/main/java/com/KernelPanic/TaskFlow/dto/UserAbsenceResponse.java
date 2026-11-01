package com.KernelPanic.TaskFlow.dto;
import com.KernelPanic.TaskFlow.entity.UserAbsence;
import java.time.Instant;
import java.time.LocalDate;
public record UserAbsenceResponse(Long id, Long userId, String userName, LocalDate startDate, LocalDate endDate, String reason, String status, Long reviewerId, String reviewerName, String reviewNote, Instant createdAt, Instant updatedAt) {
    public static UserAbsenceResponse fromEntity(UserAbsence a) { return new UserAbsenceResponse(a.getId(), a.getUser().getId(), a.getUser().getName(), a.getStartDate(), a.getEndDate(), a.getReason(), a.getStatus().name(), a.getReviewer() == null ? null : a.getReviewer().getId(), a.getReviewer() == null ? null : a.getReviewer().getName(), a.getReviewNote(), a.getCreatedAt(), a.getUpdatedAt()); }
}