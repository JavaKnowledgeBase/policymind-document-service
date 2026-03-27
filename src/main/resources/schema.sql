CREATE EXTENSION IF NOT EXISTS vector;

ALTER TABLE document_chunk
    ADD COLUMN IF NOT EXISTS embedding_vector vector(1536);

UPDATE document_chunk
SET embedding_vector = CAST(embedding AS vector)
WHERE embedding_vector IS NULL
  AND embedding IS NOT NULL
  AND embedding <> ''
  AND jsonb_typeof(embedding::jsonb) = 'array'
  AND jsonb_array_length(embedding::jsonb) = 1536;

CREATE INDEX IF NOT EXISTS idx_document_chunk_document_id
    ON document_chunk (document_id);

CREATE INDEX IF NOT EXISTS idx_document_chunk_embedding_vector_hnsw
    ON document_chunk USING hnsw (embedding_vector vector_cosine_ops);

CREATE TABLE IF NOT EXISTS policy_draft (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255),
    policy_type VARCHAR(255),
    mode VARCHAR(64),
    provider VARCHAR(64),
    jurisdiction VARCHAR(255),
    audience VARCHAR(255),
    tone VARCHAR(255),
    current_version_number INTEGER,
    latest_quality_score INTEGER,
    latest_confidence VARCHAR(64),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    last_generated_at TIMESTAMP,
    goals TEXT,
    additional_instructions TEXT,
    source_text TEXT,
    working_draft TEXT,
    latest_summary TEXT,
    latest_rationale TEXT
);

CREATE TABLE IF NOT EXISTS policy_draft_version (
    id BIGSERIAL PRIMARY KEY,
    draft_id BIGINT REFERENCES policy_draft(id) ON DELETE CASCADE,
    version_number INTEGER,
    quality_score INTEGER,
    confidence VARCHAR(64),
    created_at TIMESTAMP,
    summary TEXT,
    rationale TEXT,
    source_text TEXT,
    working_draft TEXT,
    key_changes_json TEXT,
    implementation_checklist_json TEXT,
    risk_flags_json TEXT,
    compose_result_json TEXT
);

CREATE INDEX IF NOT EXISTS idx_policy_draft_updated_at
    ON policy_draft (updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_policy_draft_version_draft_id_version_number
    ON policy_draft_version (draft_id, version_number DESC);
