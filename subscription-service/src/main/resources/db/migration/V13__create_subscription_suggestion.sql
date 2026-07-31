ALTER TABLE inbound_email
    ADD CONSTRAINT uq_inbound_email_id_user UNIQUE (id, user_id);

CREATE TABLE subscription_suggestion (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    inbound_email_id UUID NOT NULL,
    possible_duplicate_subscription_id BIGINT REFERENCES subscription(id) ON DELETE SET NULL,
    confirmed_subscription_id BIGINT REFERENCES subscription(id) ON DELETE SET NULL,
    provider VARCHAR(120) NOT NULL,
    plan_name VARCHAR(120),
    amount NUMERIC(19, 4),
    currency VARCHAR(3),
    billing_interval_unit VARCHAR(10),
    billing_interval_count INTEGER,
    renewal_date DATE,
    event_type VARCHAR(30) NOT NULL,
    confidence NUMERIC(5, 4) NOT NULL,
    evidence_summary VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    decided_at TIMESTAMPTZ,
    CONSTRAINT fk_subscription_suggestion_inbound_owner
        FOREIGN KEY (inbound_email_id, user_id)
        REFERENCES inbound_email (id, user_id)
        ON DELETE CASCADE,
    CONSTRAINT uq_subscription_suggestion_inbound UNIQUE (inbound_email_id),
    CONSTRAINT chk_subscription_suggestion_amount
        CHECK (amount IS NULL OR amount > 0),
    CONSTRAINT chk_subscription_suggestion_currency
        CHECK (currency IS NULL OR currency ~ '^[A-Z]{3}$'),
    CONSTRAINT chk_subscription_suggestion_money_pair
        CHECK ((amount IS NULL) = (currency IS NULL)),
    CONSTRAINT chk_subscription_suggestion_billing_unit
        CHECK (
            billing_interval_unit IS NULL
            OR billing_interval_unit IN ('DAY', 'WEEK', 'MONTH', 'YEAR')
        ),
    CONSTRAINT chk_subscription_suggestion_billing_pair
        CHECK (
            (billing_interval_unit IS NULL AND billing_interval_count IS NULL)
            OR (
                billing_interval_unit IS NOT NULL
                AND billing_interval_count IS NOT NULL
                AND billing_interval_count > 0
            )
        ),
    CONSTRAINT chk_subscription_suggestion_event_type
        CHECK (event_type IN (
            'NEW_SUBSCRIPTION',
            'RENEWAL_PAYMENT',
            'UPCOMING_RENEWAL',
            'PRICE_CHANGE',
            'CANCELLATION',
            'GMAIL_VERIFICATION'
        )),
    CONSTRAINT chk_subscription_suggestion_confidence
        CHECK (confidence >= 0 AND confidence <= 1),
    CONSTRAINT chk_subscription_suggestion_status
        CHECK (status IN ('PENDING', 'CONFIRMED', 'IGNORED')),
    CONSTRAINT chk_subscription_suggestion_decision_time
        CHECK (
            (status = 'PENDING' AND decided_at IS NULL)
            OR (status IN ('CONFIRMED', 'IGNORED') AND decided_at IS NOT NULL)
        ),
    CONSTRAINT chk_subscription_suggestion_updated_time
        CHECK (updated_at >= created_at),
    CONSTRAINT chk_subscription_suggestion_decided_time
        CHECK (decided_at IS NULL OR decided_at >= created_at)
);

CREATE INDEX idx_subscription_suggestion_user_status_created
    ON subscription_suggestion (user_id, status, created_at DESC);

CREATE INDEX idx_subscription_suggestion_possible_duplicate
    ON subscription_suggestion (possible_duplicate_subscription_id)
    WHERE possible_duplicate_subscription_id IS NOT NULL;
