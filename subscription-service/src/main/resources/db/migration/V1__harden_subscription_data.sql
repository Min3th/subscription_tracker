ALTER TABLE subscription DROP CONSTRAINT IF EXISTS subscription_type_check;
ALTER TABLE subscription DROP CONSTRAINT IF EXISTS subscription_cost_positive;
ALTER TABLE subscription DROP CONSTRAINT IF EXISTS subscription_category_check;
ALTER TABLE subscription DROP CONSTRAINT IF EXISTS subscription_billing_check;

UPDATE subscription
SET name = LEFT(TRIM(COALESCE(name, 'Untitled')), 120),
    cost = CASE WHEN cost IS NULL OR cost <= 0 THEN 0.01 ELSE cost END,
    type = CASE WHEN LOWER(TRIM(type)) = 'recurring' THEN 'RECURRING' ELSE 'ONE_TIME' END,
    duration = NULLIF(LEFT(TRIM(duration), 50), ''),
    category = CASE LOWER(TRIM(COALESCE(category, 'other')))
        WHEN 'general' THEN 'GENERAL'
        WHEN 'entertainment' THEN 'ENTERTAINMENT'
        WHEN 'productivity' THEN 'PRODUCTIVITY'
        WHEN 'music' THEN 'MUSIC'
        WHEN 'software' THEN 'SOFTWARE'
        WHEN 'utilities' THEN 'UTILITIES'
        WHEN 'education' THEN 'EDUCATION'
        WHEN 'health' THEN 'HEALTH'
        WHEN 'fitness' THEN 'FITNESS'
        WHEN 'news' THEN 'NEWS'
        WHEN 'cloud storage' THEN 'CLOUD_STORAGE'
        WHEN 'cloud_storage' THEN 'CLOUD_STORAGE'
        WHEN 'finance' THEN 'FINANCE'
        ELSE 'OTHER'
    END,
    description = NULLIF(LEFT(TRIM(description), 1000), ''),
    payment_method = NULLIF(LEFT(TRIM(payment_method), 120), ''),
    website = CASE
        WHEN website IS NULL OR TRIM(website) = '' THEN NULL
        WHEN TRIM(website) ~* '^https?://[^[:space:]]+$' THEN LEFT(TRIM(website), 500)
        ELSE NULL
    END,
    start_date = COALESCE(start_date, CURRENT_DATE),
    billing_interval_unit = CASE
        WHEN LOWER(TRIM(billing_interval_unit)) = 'day' THEN 'DAY'
        WHEN LOWER(TRIM(billing_interval_unit)) = 'week' THEN 'WEEK'
        WHEN LOWER(TRIM(billing_interval_unit)) = 'year' THEN 'YEAR'
        ELSE 'MONTH'
    END,
    billing_interval_count = CASE
        WHEN billing_interval_count IS NULL OR billing_interval_count <= 0 THEN 1
        ELSE billing_interval_count
    END,
    email_notifications_enabled = COALESCE(email_notifications_enabled, FALSE);

UPDATE subscription
SET billing_interval_unit = NULL,
    billing_interval_count = NULL
WHERE type = 'ONE_TIME';

ALTER TABLE subscription ALTER COLUMN name TYPE VARCHAR(120);
ALTER TABLE subscription ALTER COLUMN name SET NOT NULL;
ALTER TABLE subscription ALTER COLUMN cost SET NOT NULL;
ALTER TABLE subscription ALTER COLUMN type TYPE VARCHAR(20);
ALTER TABLE subscription ALTER COLUMN type SET NOT NULL;
ALTER TABLE subscription ALTER COLUMN duration TYPE VARCHAR(50);
ALTER TABLE subscription ALTER COLUMN category TYPE VARCHAR(30);
ALTER TABLE subscription ALTER COLUMN category SET NOT NULL;
ALTER TABLE subscription ALTER COLUMN description TYPE VARCHAR(1000);
ALTER TABLE subscription ALTER COLUMN payment_method TYPE VARCHAR(120);
ALTER TABLE subscription ALTER COLUMN website TYPE VARCHAR(500);
ALTER TABLE subscription ALTER COLUMN start_date SET NOT NULL;
ALTER TABLE subscription ALTER COLUMN billing_interval_unit TYPE VARCHAR(10);
ALTER TABLE subscription ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE subscription ALTER COLUMN email_notifications_enabled SET NOT NULL;

ALTER TABLE subscription ADD CONSTRAINT subscription_cost_positive CHECK (cost > 0);
ALTER TABLE subscription ADD CONSTRAINT subscription_type_check CHECK (type IN ('ONE_TIME', 'RECURRING'));
ALTER TABLE subscription ADD CONSTRAINT subscription_category_check CHECK (category IN (
    'GENERAL', 'ENTERTAINMENT', 'PRODUCTIVITY', 'MUSIC', 'SOFTWARE', 'UTILITIES',
    'EDUCATION', 'HEALTH', 'FITNESS', 'NEWS', 'CLOUD_STORAGE', 'FINANCE', 'OTHER'
));
ALTER TABLE subscription ADD CONSTRAINT subscription_billing_check CHECK (
    (type = 'ONE_TIME' AND billing_interval_unit IS NULL AND billing_interval_count IS NULL)
    OR
    (type = 'RECURRING' AND billing_interval_unit IN ('DAY', 'WEEK', 'MONTH', 'YEAR') AND billing_interval_count > 0)
);
