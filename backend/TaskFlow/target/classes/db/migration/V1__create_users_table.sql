CREATE TABLE users (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(120) NOT NULL,
    email          VARCHAR(180) NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,
    role           VARCHAR(30)  NOT NULL DEFAULT 'USER',
    created_at     TIMESTAMP    NOT NULL,
    updated_at     TIMESTAMP    NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_users_email ON users (email);
