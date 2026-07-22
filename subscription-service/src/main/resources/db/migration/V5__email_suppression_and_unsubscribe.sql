CREATE TABLE email_suppression (
    id BIGSERIAL PRIMARY KEY,
    email_normalized VARCHAR(320) NOT NULL UNIQUE,
    reason VARCHAR(30) NOT NULL,
    source VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE notification_unsubscribe_token (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ
);

CREATE INDEX idx_unsubscribe_token_active
    ON notification_unsubscribe_token (token_hash, expires_at)
    WHERE used_at IS NULL;
