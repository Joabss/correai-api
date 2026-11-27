ALTER TABLE goals ADD COLUMN achieved_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE goals ADD COLUMN achieved_period_start DATE;
ALTER TABLE goals ADD COLUMN achieved_current DOUBLE PRECISION;
ALTER TABLE goals ADD CONSTRAINT ck_goals_achievement_complete CHECK (
    (achieved_at IS NULL AND achieved_period_start IS NULL AND achieved_current IS NULL)
    OR (achieved_at IS NOT NULL AND achieved_period_start IS NOT NULL AND achieved_current IS NOT NULL)
);
CREATE INDEX idx_goals_user_achievements ON goals (user_id, achieved_at DESC)
    WHERE achieved_at IS NOT NULL;
