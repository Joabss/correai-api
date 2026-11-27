CREATE TABLE ${flyway:defaultSchema}.user_badges (
    id          UUID PRIMARY KEY,
    user_id     UUID         NOT NULL,
    badge_code  VARCHAR(40)  NOT NULL,
    unlocked_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_user_badges_user_code UNIQUE (user_id, badge_code)
);

CREATE INDEX idx_user_badges_user ON ${flyway:defaultSchema}.user_badges (user_id);
