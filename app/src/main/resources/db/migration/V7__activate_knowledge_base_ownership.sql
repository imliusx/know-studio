ALTER TABLE documents ALTER COLUMN workspace_id DROP NOT NULL;
ALTER TABLE document_chunks ALTER COLUMN workspace_id DROP NOT NULL;
ALTER TABLE chunk_embeddings ALTER COLUMN workspace_id DROP NOT NULL;
ALTER TABLE ingestion_jobs ALTER COLUMN workspace_id DROP NOT NULL;
ALTER TABLE upload_sessions ALTER COLUMN workspace_id DROP NOT NULL;
ALTER TABLE eval_datasets ALTER COLUMN workspace_id DROP NOT NULL;
ALTER TABLE eval_samples ALTER COLUMN workspace_id DROP NOT NULL;
ALTER TABLE eval_runs ALTER COLUMN workspace_id DROP NOT NULL;

ALTER TABLE documents ALTER COLUMN knowledge_base_id SET NOT NULL;
ALTER TABLE document_chunks ALTER COLUMN knowledge_base_id SET NOT NULL;
ALTER TABLE chunk_embeddings ALTER COLUMN knowledge_base_id SET NOT NULL;
ALTER TABLE ingestion_jobs ALTER COLUMN knowledge_base_id SET NOT NULL;
ALTER TABLE upload_sessions ALTER COLUMN knowledge_base_id SET NOT NULL;
ALTER TABLE eval_datasets ALTER COLUMN knowledge_base_id SET NOT NULL;
ALTER TABLE eval_samples ALTER COLUMN knowledge_base_id SET NOT NULL;
ALTER TABLE eval_runs ALTER COLUMN knowledge_base_id SET NOT NULL;

DROP INDEX uk_documents_workspace_hash_active;
CREATE UNIQUE INDEX uk_documents_knowledge_base_hash_active
    ON documents(knowledge_base_id, content_hash)
    WHERE status <> 'DELETED';
