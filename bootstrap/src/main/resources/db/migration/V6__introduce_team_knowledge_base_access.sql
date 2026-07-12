CREATE TABLE teams (
    id BIGINT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    parent_id BIGINT REFERENCES teams(id),
    status VARCHAR(32) NOT NULL,
    created_by BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE team_members (
    id BIGINT PRIMARY KEY,
    team_id BIGINT NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    team_role VARCHAR(32) NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_team_members_team_user UNIQUE (team_id, user_id)
);

CREATE INDEX idx_team_members_user ON team_members(user_id, team_id);

CREATE TABLE knowledge_bases (
    id BIGINT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    visibility VARCHAR(32) NOT NULL,
    owner_team_id BIGINT REFERENCES teams(id),
    created_by BIGINT NOT NULL REFERENCES users(id),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE knowledge_base_team_grants (
    id BIGINT PRIMARY KEY,
    knowledge_base_id BIGINT NOT NULL REFERENCES knowledge_bases(id) ON DELETE CASCADE,
    team_id BIGINT NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    permission VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_knowledge_base_team_grants UNIQUE (knowledge_base_id, team_id)
);

CREATE INDEX idx_knowledge_base_team_grants_team
    ON knowledge_base_team_grants(team_id, knowledge_base_id);

INSERT INTO teams (id, name, description, status, created_by, created_at, updated_at)
SELECT id, name, description, status, owner_id, created_at, updated_at
FROM workspaces;

INSERT INTO team_members (id, team_id, user_id, team_role, joined_at)
SELECT
    id,
    workspace_id,
    user_id,
    CASE
        WHEN workspace_role IN ('OWNER', 'ADMIN') THEN 'TEAM_ADMIN'
        ELSE 'MEMBER'
    END,
    joined_at
FROM workspace_members;

INSERT INTO knowledge_bases (
    id,
    name,
    description,
    visibility,
    owner_team_id,
    created_by,
    status,
    created_at,
    updated_at
)
SELECT
    id,
    name,
    description,
    'TEAM',
    id,
    owner_id,
    status,
    created_at,
    updated_at
FROM workspaces;

INSERT INTO knowledge_base_team_grants (
    id,
    knowledge_base_id,
    team_id,
    permission,
    created_at
)
SELECT id, id, id, 'MANAGE', created_at
FROM workspaces;

ALTER TABLE documents
    ADD COLUMN knowledge_base_id BIGINT REFERENCES knowledge_bases(id);
UPDATE documents SET knowledge_base_id = workspace_id;
CREATE INDEX idx_documents_knowledge_base_status
    ON documents(knowledge_base_id, status, created_at DESC);

ALTER TABLE document_chunks
    ADD COLUMN knowledge_base_id BIGINT REFERENCES knowledge_bases(id);
UPDATE document_chunks SET knowledge_base_id = workspace_id;
CREATE INDEX idx_document_chunks_knowledge_base_document
    ON document_chunks(knowledge_base_id, document_id, chunk_index);

ALTER TABLE chunk_embeddings
    ADD COLUMN knowledge_base_id BIGINT REFERENCES knowledge_bases(id);
UPDATE chunk_embeddings SET knowledge_base_id = workspace_id;
CREATE INDEX idx_chunk_embeddings_knowledge_base
    ON chunk_embeddings(knowledge_base_id, document_id);

ALTER TABLE ingestion_jobs
    ADD COLUMN knowledge_base_id BIGINT REFERENCES knowledge_bases(id);
UPDATE ingestion_jobs SET knowledge_base_id = workspace_id;

ALTER TABLE upload_sessions
    ADD COLUMN knowledge_base_id BIGINT REFERENCES knowledge_bases(id);
UPDATE upload_sessions SET knowledge_base_id = workspace_id;
CREATE INDEX idx_upload_sessions_knowledge_base_hash
    ON upload_sessions(knowledge_base_id, content_hash, status);

ALTER TABLE eval_datasets
    ADD COLUMN knowledge_base_id BIGINT REFERENCES knowledge_bases(id);
UPDATE eval_datasets SET knowledge_base_id = workspace_id;
CREATE INDEX idx_eval_datasets_knowledge_base_user
    ON eval_datasets(knowledge_base_id, user_id, created_at DESC);

ALTER TABLE eval_samples
    ADD COLUMN knowledge_base_id BIGINT REFERENCES knowledge_bases(id);
UPDATE eval_samples SET knowledge_base_id = workspace_id;
CREATE INDEX idx_eval_samples_knowledge_base_dataset
    ON eval_samples(knowledge_base_id, dataset_id, created_at, id);

ALTER TABLE eval_runs
    ADD COLUMN knowledge_base_id BIGINT REFERENCES knowledge_bases(id);
UPDATE eval_runs SET knowledge_base_id = workspace_id;
CREATE INDEX idx_eval_runs_knowledge_base_dataset_created
    ON eval_runs(knowledge_base_id, dataset_id, created_at DESC);
