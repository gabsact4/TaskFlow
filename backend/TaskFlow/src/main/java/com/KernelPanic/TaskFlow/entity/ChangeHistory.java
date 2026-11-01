package com.KernelPanic.TaskFlow.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "change_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChangeHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "project_id", nullable = false) private Long projectId;
    @Column(name = "project_owner_id", nullable = false) private Long projectOwnerId;
    @Column(name = "actor_id", nullable = false) private Long actorId;
    @Column(name = "actor_name", nullable = false, length = 1024) private String actorName;
    @Column(name = "entity_type", nullable = false, length = 30) private String entityType;
    @Column(name = "entity_id", nullable = false) private Long entityId;
    @Column(name = "task_id") private Long taskId;
    @Column(nullable = false, length = 30) private String action;
    @Column(nullable = false, length = 500) private String summary;
    @Column(name = "details_json", columnDefinition = "json") private String detailsJson;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @PrePersist protected void onCreate() { if (createdAt == null) createdAt = Instant.now(); }
}