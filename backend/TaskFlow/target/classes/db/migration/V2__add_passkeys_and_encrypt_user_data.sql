ALTER TABLE users
    MODIFY COLUMN name VARCHAR(1024) NOT NULL,
    MODIFY COLUMN email VARCHAR(512) NOT NULL,
    ADD COLUMN webauthn_user_handle VARCHAR(64) NULL,
    ADD COLUMN email_lookup_hash VARCHAR(64) NULL,
    ADD CONSTRAINT uk_users_webauthn_user_handle UNIQUE (webauthn_user_handle),
    ADD CONSTRAINT uk_users_email_lookup_hash UNIQUE (email_lookup_hash);

CREATE TABLE passkey_credentials (
    credential_id   VARBINARY(1024) PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    user_handle     VARCHAR(64) NOT NULL,
    public_key_cose VARCHAR(4096) NOT NULL,
    signature_count BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_passkey_credentials_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_passkey_credentials_user_id ON passkey_credentials (user_id);

CREATE TABLE webauthn_challenges (
    challenge_id CHAR(36) PRIMARY KEY,
    user_id      BIGINT NULL,
    ceremony     VARCHAR(20) NOT NULL,
    request_json TEXT NOT NULL,
    expires_at   TIMESTAMP NOT NULL,
    CONSTRAINT fk_webauthn_challenges_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_webauthn_challenges_expires_at ON webauthn_challenges (expires_at);