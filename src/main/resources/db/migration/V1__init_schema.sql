CREATE TABLE IF NOT EXISTS users (
    id         UUID PRIMARY KEY,
    created_at TIMESTAMP(6) WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS activities (
    id                UUID PRIMARY KEY,
    user_id           UUID NOT NULL,
    type              VARCHAR(255),
    activity_date     DATE,
    distance_km       DOUBLE PRECISION,
    duration_seconds  INTEGER,
    avg_pace_seconds  INTEGER,
    training_type     VARCHAR(255),
    perceived_effort  VARCHAR(255),
    notes             VARCHAR(255),
    created_at        TIMESTAMP(6) WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_activities_user_date ON activities (user_id, activity_date DESC);
