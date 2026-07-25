ALTER TABLE subscription
    DROP CONSTRAINT subscription_billing_check;

ALTER TABLE subscription
    ADD CONSTRAINT subscription_billing_check CHECK (
        (
            type = 'ONE_TIME'
            AND billing_interval_unit IS NULL
            AND billing_interval_count IS NULL
        )
        OR
        (
            type = 'RECURRING'
            AND billing_interval_unit IS NOT NULL
            AND billing_interval_count IS NOT NULL
            AND billing_interval_unit IN ('DAY', 'WEEK', 'MONTH', 'YEAR')
            AND billing_interval_count > 0
        )
    );
