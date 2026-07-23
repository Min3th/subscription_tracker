CREATE TABLE refresh_token_sessions (
    token_id VARCHAR(36) PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    replaced_by_token_id VARCHAR(36)
);

CREATE INDEX idx_refresh_session_user
    ON refresh_token_sessions (user_id);

CREATE INDEX idx_refresh_session_expires
    ON refresh_token_sessions (expires_at);
