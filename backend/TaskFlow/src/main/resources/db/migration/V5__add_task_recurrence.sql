ALTER TABLE tasks
    ADD COLUMN recurrence VARCHAR(20) NOT NULL DEFAULT 'NONE',
    ADD COLUMN recurrence_end_date DATE NULL,
    ADD COLUMN next_occurrence_date DATE NULL;

CREATE INDEX idx_tasks_recurrence_date ON tasks (recurrence, next_occurrence_date);
