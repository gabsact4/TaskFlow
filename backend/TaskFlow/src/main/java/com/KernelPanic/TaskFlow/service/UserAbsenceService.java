package com.KernelPanic.TaskFlow.service;

import com.KernelPanic.TaskFlow.dto.CreateAbsenceRequest;
import com.KernelPanic.TaskFlow.dto.ReviewAbsenceRequest;
import com.KernelPanic.TaskFlow.dto.UserAbsenceResponse;
import com.KernelPanic.TaskFlow.entity.Project;
import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.entity.UserAbsence;
import com.KernelPanic.TaskFlow.enums.Role;
import com.KernelPanic.TaskFlow.repository.ProjectRepository;
import com.KernelPanic.TaskFlow.repository.TaskRepository;
import com.KernelPanic.TaskFlow.repository.UserAbsenceRepository;
import com.KernelPanic.TaskFlow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.List;

@Service @RequiredArgsConstructor
public class UserAbsenceService {
    private static final List<UserAbsence.AbsenceStatus> BLOCKING = List.of(UserAbsence.AbsenceStatus.PENDING, UserAbsence.AbsenceStatus.APPROVED);
    private final UserAbsenceRepository absences;
    private final UserRepository users;
    private final ProjectRepository projects;
    private final TaskRepository tasks;

    @Transactional(readOnly = true)
    public List<UserAbsenceResponse> list(User current) {
        if (isMaster(current)) return absences.findAll().stream().sorted((left, right) -> left.getStartDate().compareTo(right.getStartDate())).map(absence -> UserAbsenceResponse.fromEntity(absence)).toList();
        if (current.getRole() == Role.PO) {
            return absences.findAll().stream().filter(a -> a.getUser().getId().equals(current.getId()) || canReview(a.getUser(), current))
                    .sorted((left, right) -> left.getStartDate().compareTo(right.getStartDate())).map(absence -> UserAbsenceResponse.fromEntity(absence)).toList();
        }
        return absences.findByUserIdOrderByStartDateDesc(current.getId()).stream().map(UserAbsenceResponse::fromEntity).toList();
    }

    @Transactional
    public UserAbsenceResponse request(CreateAbsenceRequest request, User current) {
        validateDates(request.startDate(), request.endDate());
        if (request.startDate().isBefore(LocalDate.now())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data inicial da ausência não pode estar no passado.");
        User lockedUser = users.findByIdForUpdate(current.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        if (absences.existsByUserIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(lockedUser.getId(), BLOCKING, request.endDate(), request.startDate())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma solicitação pendente ou aprovada nesse período.");
        }
        UserAbsence absence = UserAbsence.builder().user(lockedUser).startDate(request.startDate()).endDate(request.endDate())
                .reason(normalize(request.reason())).status(UserAbsence.AbsenceStatus.PENDING).build();
        return UserAbsenceResponse.fromEntity(absences.save(absence));
    }

    @Transactional
    public UserAbsenceResponse review(Long id, ReviewAbsenceRequest request, User reviewer) {
        UserAbsence absence = find(id);
        if (!isMaster(reviewer) && !canReview(absence.getUser(), reviewer)) throw new AccessDeniedException("Somente o Master ou o PO responsável pela equipe pode revisar esta solicitação.");
        if (absence.getStatus() != UserAbsence.AbsenceStatus.PENDING) throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta solicitação já foi revisada.");
        if (request.approved() && !absences.findConflicts(absence.getUser().getId(), id, BLOCKING, absence.getStartDate(), absence.getEndDate()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Há outra solicitação pendente ou aprovada que se sobrepõe a este período.");
        }
        absence.setStatus(request.approved() ? UserAbsence.AbsenceStatus.APPROVED : UserAbsence.AbsenceStatus.REJECTED);
        absence.setReviewer(reviewer);
        absence.setReviewNote(normalize(request.note()));
        return UserAbsenceResponse.fromEntity(absences.save(absence));
    }

    @Transactional
    public void cancel(Long id, User current) {
        UserAbsence absence = find(id);
        boolean reviewer = isMaster(current) || canReview(absence.getUser(), current);
        if (!absence.getUser().getId().equals(current.getId()) && !reviewer) throw new AccessDeniedException("Você não pode cancelar esta solicitação.");
        if (absence.getStatus() == UserAbsence.AbsenceStatus.APPROVED && !reviewer) throw new AccessDeniedException("Uma ausência aprovada só pode ser cancelada pelo responsável ou pelo Master.");
        absences.delete(absence);
    }

    @Transactional(readOnly = true)
    public List<UserAbsence> approvedOverlapping(LocalDate start, LocalDate end) {
        return absences.findByStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(List.of(UserAbsence.AbsenceStatus.APPROVED), end, start);
    }

    private boolean canReview(User employee, User reviewer) {
        if (reviewer.getRole() != Role.PO) return false;
        for (Project project : projects.findByOwnerIdOrderByUpdatedAtDesc(reviewer.getId())) {
            if (project.getMembers().stream().anyMatch(member -> member.getId().equals(employee.getId()))
                    || tasks.existsByProjectIdAndAssigneeId(project.getId(), employee.getId())) return true;
        }
        return false;
    }
    private UserAbsence find(Long id) { return absences.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação de ausência não encontrada.")); }
    private void validateDates(LocalDate start, LocalDate end) { if (start.isAfter(end)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data final deve ser igual ou posterior à inicial."); }
    private String normalize(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private boolean isMaster(User user) { return user.getRole() == Role.MASTER || user.getRole() == Role.ADMIN; }
}