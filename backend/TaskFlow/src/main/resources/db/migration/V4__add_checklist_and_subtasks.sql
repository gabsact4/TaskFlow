ALTER TABLE tasks
    ADD COLUMN parent_task_id BIGINT NULL,
    ADD CONSTRAINT fk_tasks_parent_task FOREIGN KEY (parent_task_id)
        REFERENCES tasks (id) ON DELETE CASCADE;

CREATE INDEX idx_tasks_parent_task_id ON tasks (parent_task_id);

CREATE TABLE task_checklist_items (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id   BIGINT NOT NULL,
    text      VARCHAR(300) NOT NULL,
    done      BOOLEAN NOT NULL DEFAULT FALSE,
    position  INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_checklist_task FOREIGN KEY (task_id)
        REFERENCES tasks (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_checklist_task_position ON task_checklist_items (task_id, position, id);
