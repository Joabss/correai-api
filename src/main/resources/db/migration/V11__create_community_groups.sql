CREATE TABLE ${flyway:defaultSchema}.community_groups (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    created_by UUID NOT NULL REFERENCES ${flyway:defaultSchema}.users(id),
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

CREATE TABLE ${flyway:defaultSchema}.community_group_members (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL REFERENCES ${flyway:defaultSchema}.community_groups(id),
    user_id UUID NOT NULL REFERENCES ${flyway:defaultSchema}.users(id),
    CONSTRAINT uk_community_group_member UNIQUE (group_id, user_id)
);

CREATE INDEX idx_community_group_members_user ON ${flyway:defaultSchema}.community_group_members (user_id);
