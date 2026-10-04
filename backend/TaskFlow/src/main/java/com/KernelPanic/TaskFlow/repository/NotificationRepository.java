package com.KernelPanic.TaskFlow.repository;

import com.KernelPanic.TaskFlow.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop100ByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Notification> findByIdAndUserId(Long id, Long userId);
    boolean existsByEventKey(String eventKey);
    long countByUserIdAndReadAtIsNull(Long userId);

    @Modifying
    @Transactional
    @Query("update Notification n set n.readAt = :readAt where n.user.id = :userId and n.readAt is null")
    int markAllRead(@Param("userId") Long userId, @Param("readAt") java.time.Instant readAt);

    @Modifying
    @Transactional
    @Query(value = "insert ignore into notifications (user_id, task_id, type, title, message, event_key, created_at) values (:userId, :taskId, :type, :title, :message, :eventKey, :createdAt)", nativeQuery = true)
    int insertIfAbsent(@Param("userId") Long userId, @Param("taskId") Long taskId,
                       @Param("type") String type, @Param("title") String title,
                       @Param("message") String message, @Param("eventKey") String eventKey,
                       @Param("createdAt") Instant createdAt);
}
