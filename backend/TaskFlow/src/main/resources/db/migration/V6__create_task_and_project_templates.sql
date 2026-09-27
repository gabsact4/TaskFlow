CREATE TABLE task_templates (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    name              VARCHAR(80) NOT NULL,
    title             VARCHAR(180) NOT NULL,
    description       VARCHAR(2000) NULL,
    priority          VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    recurrence        VARCHAR(20) NOT NULL DEFAULT 'NONE',
    recurrence_end_date DATE NULL,
    checklist_items   TEXT NULL,
    owner_id          BIGINT NOT NULL,
    created_at        TIMESTAMP NOT NULL,
    CONSTRAINT fk_task_templates_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
CREATE INDEX idx_task_templates_owner ON task_templates (owner_id, created_at);

CREATE TABLE project_templates (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(80) NOT NULL,
    project_name  VARCHAR(120) NOT NULL,
    description   VARCHAR(1000) NULL,
    starter_tasks TEXT NULL,
    owner_id      BIGINT NOT NULL,
    created_at    TIMESTAMP NOT NULL,
    CONSTRAINT fk_project_templates_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
CREATE INDEX idx_project_templates_owner ON project_templates (owner_id, created_at);
