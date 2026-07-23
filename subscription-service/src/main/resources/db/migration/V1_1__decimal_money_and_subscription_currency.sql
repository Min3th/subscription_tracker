ALTER TABLE subscription
    ALTER COLUMN cost TYPE NUMERIC(19,4)
    USING ROUND(cost::numeric, 4);

ALTER TABLE subscription
    ADD COLUMN IF NOT EXISTS currency VARCHAR(3);

UPDATE subscription subscription_record
SET currency = COALESCE(
    (
        SELECT UPPER(preferences.currency)
        FROM user_preferences preferences
        WHERE preferences.user_id = subscription_record.user_id
          AND preferences.currency ~ '^[A-Za-z]{3}$'
    ),
    'USD'
)
WHERE currency IS NULL OR currency = '';

ALTER TABLE subscription ALTER COLUMN currency SET NOT NULL;
ALTER TABLE subscription DROP CONSTRAINT IF EXISTS subscription_cost_positive;
ALTER TABLE subscription DROP CONSTRAINT IF EXISTS subscription_currency_format;
ALTER TABLE subscription ADD CONSTRAINT subscription_cost_positive CHECK (cost > 0);
ALTER TABLE subscription ADD CONSTRAINT subscription_currency_format CHECK (currency ~ '^[A-Z]{3}$');
