ALTER TABLE subscription_suggestion
    ADD COLUMN action_url VARCHAR(2000);

ALTER TABLE subscription_suggestion
    ADD CONSTRAINT chk_subscription_suggestion_action_url
        CHECK (
            action_url IS NULL
            OR (
                event_type = 'GMAIL_VERIFICATION'
                AND action_url LIKE 'https://mail-settings.google.com/mail/vf-%'
            )
        );
