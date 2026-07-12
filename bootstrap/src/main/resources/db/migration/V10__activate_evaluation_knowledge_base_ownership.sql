ALTER TABLE eval_datasets ALTER COLUMN knowledge_base_id SET NOT NULL;
ALTER TABLE eval_samples ALTER COLUMN knowledge_base_id SET NOT NULL;
ALTER TABLE eval_runs ALTER COLUMN knowledge_base_id SET NOT NULL;

ALTER TABLE eval_datasets DROP CONSTRAINT uk_eval_datasets_workspace_name;
CREATE UNIQUE INDEX uk_eval_datasets_knowledge_base_name
    ON eval_datasets(knowledge_base_id, name);

DROP INDEX idx_eval_datasets_workspace_user;
DROP INDEX idx_eval_samples_workspace_dataset;
DROP INDEX idx_eval_runs_workspace_dataset_created;
