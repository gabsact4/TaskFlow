CREATE TABLE task_work_logs (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    duration_minutes INT NOT NULL,
    comment VARCHAR(500) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_task_work_logs_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_work_logs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_task_work_logs_task_created (task_id, created_at)
);
