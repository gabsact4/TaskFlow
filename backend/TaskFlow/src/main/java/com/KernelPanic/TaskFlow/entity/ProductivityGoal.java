package com.KernelPanic.TaskFlow.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name = "productivity_goals", uniqueConstraints = @UniqueConstraint(name = "uk_productivity_goal_user_metric_period", columnNames = {"user_id", "metric", "period_type"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductivityGoal {
    public enum Metric { TRACKED_HOURS, COMPLETED_TASKS, FOCUS_SESSIONS }
    public enum PeriodType { WEEKLY, MONTHLY }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Metric metric;
    @Enumerated(EnumType.STRING) @Column(name = "period_type", nullable = false, length = 20) private PeriodType periodType;
    @Column(name = "target_value", nullable = false, precision = 10, scale = 2) private BigDecimal targetValue;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @PrePersist protected void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = Instant.now(); }
}