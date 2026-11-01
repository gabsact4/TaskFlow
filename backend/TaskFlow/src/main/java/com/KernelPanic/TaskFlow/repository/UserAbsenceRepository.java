package com.KernelPanic.TaskFlow.repository;
import com.KernelPanic.TaskFlow.entity.UserAbsence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
public interface UserAbsenceRepository extends JpaRepository<UserAbsence, Long> {
    List<UserAbsence> findByUserIdOrderByStartDateDesc(Long userId);
    List<UserAbsence> findByStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(List<UserAbsence.AbsenceStatus> statuses, LocalDate end, LocalDate start);
    boolean existsByUserIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(Long userId, List<UserAbsence.AbsenceStatus> statuses, LocalDate end, LocalDate start);
    @Query("select a from UserAbsence a where a.user.id = :userId and a.id <> :excludeId and a.status in :statuses and a.startDate <= :end and a.endDate >= :start")
    List<UserAbsence> findConflicts(@Param("userId") Long userId, @Param("excludeId") Long excludeId,
                                    @Param("statuses") List<UserAbsence.AbsenceStatus> statuses,
                                    @Param("start") LocalDate start, @Param("end") LocalDate end);
}