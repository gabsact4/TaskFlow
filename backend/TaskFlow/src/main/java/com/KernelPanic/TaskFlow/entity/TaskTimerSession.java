package com.KernelPanic.TaskFlow.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "task_timer_sessions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TaskTimerSession {
    public enum TimerStatus { RUNNING, PAUSED, STOPPED }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "task_id", nullable = false) private Task task;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private TimerStatus status;
    @Column(name = "started_at", nullable = false) private Instant startedAt;
    @Column(name = "paused_at") private Instant pausedAt;
    @Column(name = "ended_at") private Instant endedAt;
    @Column(name = "paused_seconds", nullable = false) @Builder.Default private Long pausedSeconds = 0L;
}