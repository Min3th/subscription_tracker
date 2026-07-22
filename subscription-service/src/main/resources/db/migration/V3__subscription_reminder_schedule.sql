CREATE TABLE subscription_reminder_schedule (
    subscription_id BIGINT PRIMARY KEY REFERENCES subscription(id) ON DELETE CASCADE,
    billing_date DATE NOT NULL,
    due_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_reminder_schedule_due_at
    ON subscription_reminder_schedule (due_at);
