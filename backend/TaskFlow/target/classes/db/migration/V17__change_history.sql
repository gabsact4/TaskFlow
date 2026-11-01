CREATE TABLE change_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    project_owner_id BIGINT NOT NULL,
    actor_id BIGINT NOT NULL,
    actor_name VARCHAR(1024) NOT NULL,
    entity_type VARCHAR(30) NOT NULL,
    entity_id BIGINT NOT NULL,
    task_id BIGINT NULL,
    action VARCHAR(30) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    details_json JSON NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_change_history_project_created (project_id, created_at),
    KEY idx_change_history_project_owner_created (project_owner_id, created_at),
    KEY idx_change_history_task_created (task_id, created_at),
    KEY idx_change_history_actor_created (actor_id, created_at)
);