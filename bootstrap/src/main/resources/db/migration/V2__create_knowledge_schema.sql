CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE documents (
    id BIGINT PRIMARY KEY,
    workspace_id BIGINT NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    object_key VARCHAR(700) NOT NULL,
    content_type VARCHAR(150),
    file_size BIGINT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    preview_text TEXT,
    failure_reason VARCHAR(2000),
    chunk_count INTEGER NOT NULL DEFAULT 0,
    created_by BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_documents_workspace_hash UNIQUE (workspace_id, content_hash),
    CONSTRAINT uk_documents_object_key UNIQUE (object_key)
);

CREATE INDEX idx_documents_workspace_status ON documents(workspace_id, status, created_at DESC);

CREATE TABLE document_chunks (
    id BIGINT PRIMARY KEY,
    workspace_id BIGINT NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    document_id BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    chunk_text TEXT NOT NULL,
    char_start INTEGER NOT NULL,
    char_end INTEGER NOT NULL,
    section_path VARCHAR(500),
    status VARCHAR(32) NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_document_chunks_document_index UNIQUE (document_id, chunk_index)
);

CREATE INDEX idx_document_chunks_workspace_document ON document_chunks(workspace_id, document_id, chunk_index);

CREATE TABLE chunk_embeddings (
    chunk_id BIGINT PRIMARY KEY REFERENCES document_chunks(id) ON DELETE CASCADE,
    workspace_id BIGINT NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    document_id BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    embedding vector(1024) NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX idx_chunk_embeddings_hnsw ON chunk_embeddings USING hnsw (embedding vector_cosine_ops);
CREATE INDEX idx_chunk_embeddings_workspace ON chunk_embeddings(workspace_id, document_id);

CREATE TABLE ingestion_jobs (
    id BIGINT PRIMARY KEY,
    workspace_id BIGINT NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    document_id BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    job_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    node_logs JSONB NOT NULL DEFAULT '[]'::jsonb,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    error VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ingestion_jobs_document_type UNIQUE (document_id, job_type)
);

CREATE INDEX idx_ingestion_jobs_status ON ingestion_jobs(status, updated_at);

CREATE TABLE upload_sessions (
    id BIGINT PRIMARY KEY,
    workspace_id BIGINT NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(150),
    file_size BIGINT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    total_chunks INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_by BIGINT NOT NULL REFERENCES users(id),
    document_id BIGINT REFERENCES documents(id),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_upload_sessions_workspace_hash ON upload_sessions(workspace_id, content_hash, status);

CREATE TABLE upload_chunks (
    id BIGINT PRIMARY KEY,
    upload_session_id BIGINT NOT NULL REFERENCES upload_sessions(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    object_key VARCHAR(700) NOT NULL,
    chunk_size BIGINT NOT NULL,
    chunk_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_upload_chunks_session_index UNIQUE (upload_session_id, chunk_index),
    CONSTRAINT uk_upload_chunks_object_key UNIQUE (object_key)
);
