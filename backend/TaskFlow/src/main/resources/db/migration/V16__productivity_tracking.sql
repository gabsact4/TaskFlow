ALTER TABLE tasks ADD COLUMN completed_at TIMESTAMP(6) NULL;

CREATE TABLE task_timer_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP(6) NOT NULL,
    paused_at TIMESTAMP(6) NULL,
    ended_at TIMESTAMP(6) NULL,
    paused_seconds BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_task_timer_user_status (user_id, status),
    KEY idx_task_timer_task_started (task_id, started_at),
    CONSTRAINT fk_task_timer_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_timer_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_task_timer_status CHECK (status IN ('RUNNING', 'PAUSED', 'STOPPED'))
);

CREATE TABLE focus_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    task_id BIGINT NULL,
    started_at TIMESTAMP(6) NOT NULL,
    ended_at TIMESTAMP(6) NULL,
    planned_minutes INT NOT NULL,
    actual_minutes INT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    KEY idx_focus_user_started (user_id, started_at),
    CONSTRAINT fk_focus_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_focus_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE SET NULL,
    CONSTRAINT chk_focus_minutes CHECK (planned_minutes BETWEEN 1 AND 120 AND (actual_minutes IS NULL OR actual_minutes BETWEEN 0 AND 1440))
);

CREATE TABLE productivity_goals (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    metric VARCHAR(30) NOT NULL,
    period_type VARCHAR(20) NOT NULL,
    target_value DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_productivity_goal_user_metric_period (user_id, metric, period_type),
    CONSTRAINT fk_productivity_goals_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_productivity_goal_metric CHECK (metric IN ('TRACKED_HOURS', 'COMPLETED_TASKS', 'FOCUS_SESSIONS')),
    CONSTRAINT chk_productivity_goal_period CHECK (period_type IN ('WEEKLY', 'MONTHLY')),
    CONSTRAINT chk_productivity_goal_target CHECK (target_value > 0)
);