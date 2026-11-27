CREATE TABLE ${flyway:defaultSchema}.shared_challenges (
    id UUID PRIMARY KEY,
    creator_id UUID NOT NULL REFERENCES ${flyway:defaultSchema}.users(id),
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NOT NULL,
    audience VARCHAR(20) NOT NULL,
    group_id UUID REFERENCES ${flyway:defaultSchema}.community_groups(id),
    metric VARCHAR(20) NOT NULL,
    target DOUBLE PRECISION NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    time_zone VARCHAR(100) NOT NULL,
    badge_template_id UUID NOT NULL REFERENCES ${flyway:defaultSchema}.badge_templates(id),
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT ck_shared_challenge_audience CHECK (audience IN ('GLOBAL', 'FRIENDS', 'GROUP')),
    CONSTRAINT ck_shared_challenge_group CHECK ((audience = 'GROUP' AND group_id IS NOT NULL)
        OR (audience <> 'GROUP' AND group_id IS NULL)),
    CONSTRAINT ck_shared_challenge_metric CHECK (metric IN ('DISTANCE_KM', 'ACTIVITIES')),
    CONSTRAINT ck_shared_challenge_dates CHECK (start_date <= end_date),
    CONSTRAINT ck_shared_challenge_target CHECK (target > 0)
);

CREATE INDEX idx_shared_challenges_creator ON ${flyway:defaultSchema}.shared_challenges (creator_id, created_at);
