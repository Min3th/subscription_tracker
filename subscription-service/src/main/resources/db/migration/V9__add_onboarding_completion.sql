ALTER TABLE user_preferences
    ADD COLUMN onboarding_completed BOOLEAN;

UPDATE user_preferences
SET onboarding_completed = TRUE;

ALTER TABLE user_preferences
    ALTER COLUMN onboarding_completed SET DEFAULT FALSE,
    ALTER COLUMN onboarding_completed SET NOT NULL;
