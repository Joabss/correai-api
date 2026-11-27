ALTER TABLE ${flyway:defaultSchema}.activities
    ADD COLUMN started_at TIMESTAMP(6) WITH TIME ZONE;
