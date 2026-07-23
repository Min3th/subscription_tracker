ALTER TABLE notification_delivery
    ADD COLUMN next_attempt_at TIMESTAMPTZ,
    ADD COLUMN last_attempt_at TIMESTAMPTZ,
    ADD COLUMN dead_at TIMESTAMPTZ,
    ADD COLUMN claim_token VARCHAR(36);

UPDATE notification_delivery
SET last_attempt_at = created_at,
    status = CASE WHEN status = 'FAILED' THEN 'RETRY_SCHEDULED' ELSE status END,
    next_attempt_at = CASE WHEN status = 'FAILED' THEN NOW() ELSE next_attempt_at END;

ALTER TABLE notification_delivery
    ALTER COLUMN last_attempt_at SET NOT NULL,
    DROP CONSTRAINT notification_delivery_status_check,
    ADD CONSTRAINT notification_delivery_status_check
        CHECK (status IN ('PENDING', 'PROCESSING', 'RETRY_SCHEDULED', 'SENT', 'DEAD'));

DROP INDEX idx_notification_delivery_status_created;

CREATE INDEX idx_notification_delivery_retry_due
    ON notification_delivery (next_attempt_at)
    WHERE status = 'RETRY_SCHEDULED';

CREATE INDEX idx_notification_delivery_stale_attempt
    ON notification_delivery (last_attempt_at)
    WHERE status IN ('PENDING', 'PROCESSING');

CREATE INDEX idx_notification_delivery_claim_token
    ON notification_delivery (claim_token)
    WHERE claim_token IS NOT NULL;
