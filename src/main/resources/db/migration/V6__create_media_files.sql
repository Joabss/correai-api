CREATE TABLE media_files (
    id VARCHAR(100) PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    activity_id UUID REFERENCES activities(id) ON DELETE CASCADE,
    content_type VARCHAR(50) NOT NULL,
    content BYTEA NOT NULL
);
