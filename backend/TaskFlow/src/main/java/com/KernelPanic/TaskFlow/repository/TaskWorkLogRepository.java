package com.KernelPanic.TaskFlow.repository;

import com.KernelPanic.TaskFlow.entity.TaskWorkLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.time.Instant;

public interface TaskWorkLogRepository extends JpaRepository<TaskWorkLog, Long> {
    List<TaskWorkLog> findByTaskIdOrderByCreatedAtDesc(Long taskId);
    List<TaskWorkLog> findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(Long userId, Instant from, Instant to);

    @Query("select l from TaskWorkLog l join fetch l.task t join fetch t.project p join fetch l.user where p.id in :projectIds and l.createdAt >= :from and l.createdAt < :to")
    List<TaskWorkLog> findForProjectsInPeriod(@Param("projectIds") List<Long> projectIds, @Param("from") Instant from, @Param("to") Instant to);

    @Query("select l.task.id, sum(l.durationMinutes) from TaskWorkLog l where l.task.id in :taskIds group by l.task.id")
    List<Object[]> sumMinutesByTaskIds(@Param("taskIds") List<Long> taskIds);
}
