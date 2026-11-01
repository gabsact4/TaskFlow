ALTER TABLE tasks ADD COLUMN client_mutation_id VARCHAR(64) NULL;
CREATE UNIQUE INDEX uk_tasks_client_mutation_id ON tasks (client_mutation_id);