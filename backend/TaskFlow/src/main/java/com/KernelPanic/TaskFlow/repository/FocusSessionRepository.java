package com.KernelPanic.TaskFlow.repository;
import com.KernelPanic.TaskFlow.entity.FocusSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
public interface FocusSessionRepository extends JpaRepository<FocusSession, Long> {
    Optional<FocusSession> findFirstByUserIdAndEndedAtIsNullOrderByStartedAtDesc(Long userId);
    List<FocusSession> findByUserIdAndCompletedTrueAndEndedAtGreaterThanEqualAndEndedAtLessThan(Long userId, Instant from, Instant to);
    @Query("select f from FocusSession f join fetch f.user left join fetch f.task t left join fetch t.project p where f.user.id in :userIds and f.completed = true and f.endedAt >= :from and f.endedAt < :to")
    List<FocusSession> findCompletedForUsersInPeriod(@Param("userIds") List<Long> userIds, @Param("from") Instant from, @Param("to") Instant to);
}