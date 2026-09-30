CREATE TABLE goals (
    id         UUID PRIMARY KEY,
    user_id    UUID             NOT NULL,
    type       VARCHAR(255)     NOT NULL,
    period     VARCHAR(255)     NOT NULL,
    target     DOUBLE PRECISION NOT NULL,
    active     BOOLEAN          NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE
);

CREATE INDEX idx_goals_user_active ON goals (user_id, active);
