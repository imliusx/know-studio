ALTER TABLE eval_datasets ALTER COLUMN knowledge_base_id DROP NOT NULL;
ALTER TABLE eval_samples ALTER COLUMN knowledge_base_id DROP NOT NULL;
ALTER TABLE eval_runs ALTER COLUMN knowledge_base_id DROP NOT NULL;
