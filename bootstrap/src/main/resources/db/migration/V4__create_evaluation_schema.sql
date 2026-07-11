CREATE TABLE eval_datasets (
    id BIGINT PRIMARY KEY,
    workspace_id BIGINT NOT NULL REFERENCES workspaces(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    name VARCHAR(200) NOT NULL,
    description TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_eval_datasets_workspace_name UNIQUE (workspace_id, name)
);

CREATE INDEX idx_eval_datasets_workspace_user
    ON eval_datasets(workspace_id, user_id, created_at DESC);

CREATE TABLE eval_samples (
    id BIGINT PRIMARY KEY,
    workspace_id BIGINT NOT NULL REFERENCES workspaces(id),
    dataset_id BIGINT NOT NULL REFERENCES eval_datasets(id) ON DELETE CASCADE,
    question TEXT NOT NULL,
    relevant_chunk_ids JSONB NOT NULL,
    expected_answer TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_eval_samples_workspace_dataset
    ON eval_samples(workspace_id, dataset_id, created_at, id);

CREATE TABLE eval_runs (
    id BIGINT PRIMARY KEY,
    workspace_id BIGINT NOT NULL REFERENCES workspaces(id),
    dataset_id BIGINT NOT NULL REFERENCES eval_datasets(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    config VARCHAR(32) NOT NULL,
    recall_at_k NUMERIC(8, 6) NOT NULL,
    sample_count INTEGER NOT NULL,
    avg_latency_ms BIGINT NOT NULL,
    extra JSONB NOT NULL DEFAULT '{}'::JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_eval_runs_workspace_dataset_created
    ON eval_runs(workspace_id, dataset_id, created_at DESC);
