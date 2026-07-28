CREATE TABLE inbound_email_address (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    encrypted_token VARCHAR(512) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    CONSTRAINT chk_inbound_email_address_token_hash
        CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT chk_inbound_email_address_revocation
        CHECK (revoked_at IS NULL OR revoked_at >= created_at)
);

CREATE UNIQUE INDEX uq_inbound_email_address_active_user
    ON inbound_email_address (user_id)
    WHERE revoked_at IS NULL;

CREATE INDEX idx_inbound_email_address_user
    ON inbound_email_address (user_id);
