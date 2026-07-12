ALTER TABLE sessions ALTER COLUMN workspace_id DROP NOT NULL;

DROP INDEX idx_sessions_workspace_user_updated;
CREATE INDEX idx_sessions_user_updated
    ON sessions(user_id, updated_at DESC)
    WHERE status = 'ACTIVE';
