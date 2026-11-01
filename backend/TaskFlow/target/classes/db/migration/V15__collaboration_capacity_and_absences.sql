ALTER TABLE tasks
    ADD COLUMN planned_start_date DATE NULL,
    ADD COLUMN estimated_hours DECIMAL(7,2) NULL;

CREATE TABLE task_comments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    content VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_task_comments_task_created (task_id, created_at),
    CONSTRAINT fk_task_comments_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_comments_author FOREIGN KEY (author_id) REFERENCES users(id)
);

CREATE TABLE task_attachments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    uploader_id BIGINT NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    storage_key VARCHAR(64) NOT NULL UNIQUE,
    content_type VARCHAR(120) NOT NULL,
    size_bytes BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_task_attachments_task (task_id),
    CONSTRAINT fk_task_attachments_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_attachments_uploader FOREIGN KEY (uploader_id) REFERENCES users(id)
);

CREATE TABLE user_absences (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL,
    reviewer_id BIGINT NULL,
    review_note VARCHAR(500) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_user_absences_user_dates (user_id, start_date, end_date),
    CONSTRAINT fk_user_absences_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_absences_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id),
    CONSTRAINT chk_user_absences_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_user_absences_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);