ALTER TABLE inbound_email
    ADD COLUMN claim_token VARCHAR(36);

DROP INDEX idx_inbound_email_worker;

CREATE INDEX idx_inbound_email_worker
    ON inbound_email (status, next_attempt_at, processing_started_at, received_at)
    WHERE status IN ('RECEIVED', 'PROCESSING', 'RETRY');

CREATE INDEX idx_inbound_email_claim_token
    ON inbound_email (claim_token)
    WHERE claim_token IS NOT NULL;
