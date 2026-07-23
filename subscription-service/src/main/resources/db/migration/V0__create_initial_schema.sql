CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    google_id VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    provider VARCHAR(255) DEFAULT 'Google',
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ
);

CREATE TABLE user_preferences (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    currency VARCHAR(255),
    language VARCHAR(255),
    timezone VARCHAR(255),
    theme VARCHAR(255),
    email_notifications_enabled BOOLEAN,
    reminder_days_before INTEGER NOT NULL DEFAULT 3,
    reminder_time TIME NOT NULL DEFAULT '09:00:00'
);

CREATE TABLE subscription (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    cost DOUBLE PRECISION,
    type VARCHAR(255),
    duration VARCHAR(255),
    category VARCHAR(255),
    description TEXT,
    payment_method VARCHAR(255),
    website VARCHAR(1000),
    start_date DATE,
    billing_interval_unit VARCHAR(255),
    billing_interval_count INTEGER,
    email_notifications_enabled BOOLEAN,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE
);
