ALTER TABLE documents
    DROP CONSTRAINT uk_documents_workspace_hash;

CREATE UNIQUE INDEX uk_documents_workspace_hash_active
    ON documents(workspace_id, content_hash)
    WHERE status <> 'DELETED';
