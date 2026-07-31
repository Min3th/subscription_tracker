ALTER TABLE inbound_email
    ADD COLUMN content_purged_at TIMESTAMPTZ,
    ADD CONSTRAINT chk_inbound_email_content_purge_time
        CHECK (content_purged_at IS NULL OR content_purged_at >= received_at);

CREATE INDEX idx_inbound_email_content_retention
    ON inbound_email (received_at)
    WHERE content_purged_at IS NULL;
