CREATE TABLE IF NOT EXISTS one_time_tokens (
    id         UUID PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL,
    user_id    UUID        NOT NULL,
    type       VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_one_time_tokens_hash ON one_time_tokens (token_hash);
CREATE INDEX IF NOT EXISTS idx_one_time_tokens_user_type ON one_time_tokens (user_id, type);
