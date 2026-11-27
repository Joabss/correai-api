ALTER TABLE ${flyway:defaultSchema}.challenge_participations
    ADD COLUMN finalized BOOLEAN NOT NULL DEFAULT FALSE;
