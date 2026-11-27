CREATE TABLE ${flyway:defaultSchema}.challenge_participations (
    id UUID PRIMARY KEY,
    challenge_id UUID NOT NULL REFERENCES ${flyway:defaultSchema}.shared_challenges(id),
    user_id UUID NOT NULL REFERENCES ${flyway:defaultSchema}.users(id),
    joined_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    current_progress DOUBLE PRECISION NOT NULL,
    achieved_at TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT uk_challenge_participant UNIQUE (challenge_id, user_id),
    CONSTRAINT ck_challenge_progress_nonnegative CHECK (current_progress >= 0)
);

CREATE INDEX idx_challenge_participations_user ON ${flyway:defaultSchema}.challenge_participations (user_id);
