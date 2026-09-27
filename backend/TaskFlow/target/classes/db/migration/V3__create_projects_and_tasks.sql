CREATE TABLE projects (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    project_key VARCHAR(20) NOT NULL,
    description VARCHAR(1000) NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    owner_id    BIGINT NOT NULL,
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP NOT NULL,
    CONSTRAINT uk_projects_key UNIQUE (project_key),
    CONSTRAINT fk_projects_owner FOREIGN KEY (owner_id)
        REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_projects_owner_id ON projects (owner_id);
CREATE INDEX idx_projects_status ON projects (status);

CREATE TABLE tasks (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    title        VARCHAR(180) NOT NULL,
    description  VARCHAR(2000) NULL,
    status       VARCHAR(30) NOT NULL DEFAULT 'TODO',
    priority     VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    due_date     DATE NULL,
    project_id   BIGINT NOT NULL,
    assignee_id  BIGINT NULL,
    creator_id   BIGINT NOT NULL,
    created_at   TIMESTAMP NOT NULL,
    updated_at   TIMESTAMP NOT NULL,
    CONSTRAINT fk_tasks_project FOREIGN KEY (project_id)
        REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT fk_tasks_assignee FOREIGN KEY (assignee_id)
        REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_tasks_creator FOREIGN KEY (creator_id)
        REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_tasks_project_id ON tasks (project_id);
CREATE INDEX idx_tasks_assignee_id ON tasks (assignee_id);
CREATE INDEX idx_tasks_status ON tasks (status);
CREATE INDEX idx_tasks_due_date ON tasks (due_date);
