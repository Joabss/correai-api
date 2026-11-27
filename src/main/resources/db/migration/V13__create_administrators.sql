CREATE TABLE ${flyway:defaultSchema}.admin_state (
    id INTEGER PRIMARY KEY,
    initialized BOOLEAN NOT NULL,
    CONSTRAINT ck_admin_state_singleton CHECK (id = 1)
);

INSERT INTO ${flyway:defaultSchema}.admin_state (id, initialized) VALUES (1, FALSE);

CREATE TABLE ${flyway:defaultSchema}.administrators (
    user_id UUID PRIMARY KEY REFERENCES ${flyway:defaultSchema}.users(id),
    granted_by UUID NOT NULL REFERENCES ${flyway:defaultSchema}.users(id),
    granted_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

CREATE TABLE ${flyway:defaultSchema}.admin_audit (
    id UUID PRIMARY KEY,
    actor_id UUID NOT NULL REFERENCES ${flyway:defaultSchema}.users(id),
    target_id UUID NOT NULL REFERENCES ${flyway:defaultSchema}.users(id),
    action VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);
