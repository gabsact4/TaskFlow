CREATE TABLE master_bootstrap_state (
    id TINYINT NOT NULL,
    consumed BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id)
);

INSERT INTO master_bootstrap_state (id, consumed) VALUES (1, FALSE);
