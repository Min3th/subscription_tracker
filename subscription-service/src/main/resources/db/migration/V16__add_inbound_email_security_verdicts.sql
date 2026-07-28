ALTER TABLE inbound_email
    ADD COLUMN spam_verdict VARCHAR(30),
    ADD COLUMN virus_verdict VARCHAR(30);
