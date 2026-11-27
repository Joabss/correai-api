CREATE TABLE ${flyway:defaultSchema}.friendships (
    id UUID PRIMARY KEY,
    user_low UUID NOT NULL REFERENCES ${flyway:defaultSchema}.users(id),
    user_high UUID NOT NULL REFERENCES ${flyway:defaultSchema}.users(id),
    requester_id UUID NOT NULL REFERENCES ${flyway:defaultSchema}.users(id),
    recipient_id UUID NOT NULL REFERENCES ${flyway:defaultSchema}.users(id),
    state VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_friendship_pair UNIQUE (user_low, user_high),
    CONSTRAINT ck_friendship_participants CHECK (
        user_low <> user_high AND
        ((requester_id = user_low AND recipient_id = user_high)
        OR (requester_id = user_high AND recipient_id = user_low))
    ),
    CONSTRAINT ck_friendship_state CHECK (state IN ('PENDING', 'ACCEPTED'))
);

CREATE INDEX idx_friendships_requester ON ${flyway:defaultSchema}.friendships (requester_id);
CREATE INDEX idx_friendships_recipient ON ${flyway:defaultSchema}.friendships (recipient_id);

CREATE TABLE ${flyway:defaultSchema}.user_blocks (
    id UUID PRIMARY KEY,
    blocker_id UUID NOT NULL REFERENCES ${flyway:defaultSchema}.users(id),
    blocked_id UUID NOT NULL REFERENCES ${flyway:defaultSchema}.users(id),
    CONSTRAINT uk_user_block UNIQUE (blocker_id, blocked_id),
    CONSTRAINT ck_user_block_distinct CHECK (blocker_id <> blocked_id)
);
