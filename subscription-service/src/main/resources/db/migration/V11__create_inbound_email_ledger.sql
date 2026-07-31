ALTER TABLE inbound_email_address
    ADD CONSTRAINT uq_inbound_email_address_id_user UNIQUE (id, user_id);

CREATE TABLE inbound_email (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recipient_address_id BIGINT NOT NULL,
    provider_message_id VARCHAR(998),
    message_fingerprint VARCHAR(64) NOT NULL,
    envelope_from VARCHAR(320),
    subject VARCHAR(998),
    text_body TEXT,
    html_body TEXT,
    raw_headers TEXT,
    spam_score NUMERIC(8, 3),
    status VARCHAR(30) NOT NULL,
    failure_code VARCHAR(100),
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ,
    received_at TIMESTAMPTZ NOT NULL,
    processing_started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    CONSTRAINT fk_inbound_email_address_owner
        FOREIGN KEY (recipient_address_id, user_id)
        REFERENCES inbound_email_address (id, user_id)
        ON DELETE CASCADE,
    CONSTRAINT uq_inbound_email_recipient_fingerprint
        UNIQUE (recipient_address_id, message_fingerprint),
    CONSTRAINT chk_inbound_email_fingerprint
        CHECK (message_fingerprint ~ '^[0-9a-f]{64}$'),
    CONSTRAINT chk_inbound_email_status
        CHECK (status IN (
            'RECEIVED',
            'PROCESSING',
            'SUGGESTION_CREATED',
            'IGNORED',
            'RETRY',
            'DEAD'
        )),
    CONSTRAINT chk_inbound_email_attempt_count
        CHECK (attempt_count >= 0),
    CONSTRAINT chk_inbound_email_spam_score
        CHECK (spam_score IS NULL OR spam_score >= 0),
    CONSTRAINT chk_inbound_email_processing_time
        CHECK (processing_started_at IS NULL OR processing_started_at >= received_at),
    CONSTRAINT chk_inbound_email_completion_time
        CHECK (completed_at IS NULL OR completed_at >= received_at)
);

CREATE INDEX idx_inbound_email_user_received
    ON inbound_email (user_id, received_at DESC);

CREATE INDEX idx_inbound_email_worker
    ON inbound_email (status, next_attempt_at, received_at)
    WHERE status IN ('RECEIVED', 'RETRY');
