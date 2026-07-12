ALTER TABLE eval_samples
    ADD COLUMN expect_refusal BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE eval_samples
    ADD CONSTRAINT ck_eval_samples_refusal_chunks
        CHECK (
            (expect_refusal = TRUE AND relevant_chunk_ids = '[]'::JSONB)
            OR (expect_refusal = FALSE AND jsonb_array_length(relevant_chunk_ids) > 0)
        );
