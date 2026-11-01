package com.KernelPanic.TaskFlow.repository;
import com.KernelPanic.TaskFlow.entity.TaskTimerSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.Optional;
public interface TaskTimerSessionRepository extends JpaRepository<TaskTimerSession, Long> {
    Optional<TaskTimerSession> findFirstByUserIdAndStatusInOrderByStartedAtDesc(Long userId, Collection<TaskTimerSession.TimerStatus> statuses);
}