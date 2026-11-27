CREATE TABLE ${flyway:defaultSchema}.badge_templates (
    id UUID PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    description VARCHAR(300) NOT NULL,
    icon VARCHAR(20) NOT NULL,
    color VARCHAR(7) NOT NULL,
    created_by UUID NOT NULL REFERENCES ${flyway:defaultSchema}.users(id),
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_badge_template_icon CHECK (icon IN ('FOOTPRINT', 'FIVE_K'))
);
