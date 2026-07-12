DROP INDEX IF EXISTS idx_documents_workspace_status;
DROP INDEX IF EXISTS idx_document_chunks_workspace_document;
DROP INDEX IF EXISTS idx_chunk_embeddings_workspace;
DROP INDEX IF EXISTS idx_upload_sessions_workspace_hash;

ALTER TABLE documents DROP COLUMN workspace_id;
ALTER TABLE document_chunks DROP COLUMN workspace_id;
ALTER TABLE chunk_embeddings DROP COLUMN workspace_id;
ALTER TABLE ingestion_jobs DROP COLUMN workspace_id;
ALTER TABLE upload_sessions DROP COLUMN workspace_id;
ALTER TABLE sessions DROP COLUMN workspace_id;
ALTER TABLE eval_datasets DROP COLUMN workspace_id;
ALTER TABLE eval_samples DROP COLUMN workspace_id;
ALTER TABLE eval_runs DROP COLUMN workspace_id;

DROP TABLE workspace_members;
DROP TABLE workspaces;
