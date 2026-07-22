CREATE TABLE notification_delivery (
    id BIGSERIAL PRIMARY KEY,
    subscription_id BIGINT NOT NULL REFERENCES subscription(id) ON DELETE CASCADE,
    billing_date DATE NOT NULL,
    notification_type VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL,
    sent_at TIMESTAMPTZ,
    last_error VARCHAR(1000),
    CONSTRAINT uk_notification_delivery_idempotency
        UNIQUE (subscription_id, billing_date, notification_type),
    CONSTRAINT notification_delivery_status_check
        CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    CONSTRAINT notification_delivery_attempts_check
        CHECK (attempts > 0)
);

CREATE INDEX idx_notification_delivery_status_created
    ON notification_delivery (status, created_at);
