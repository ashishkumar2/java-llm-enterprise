-- Enable pgvector extension (idempotent)
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS documents (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    filename        VARCHAR(500) NOT NULL,
    content_type    VARCHAR(200),
    file_size_bytes BIGINT,
    status          VARCHAR(50)  NOT NULL DEFAULT 'PROCESSING',
    uploaded_by     VARCHAR(255) NOT NULL,
    error_message   TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS document_chunks (
    id          BIGSERIAL    PRIMARY KEY,
    document_id UUID         NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    chunk_index INT          NOT NULL,
    content     TEXT         NOT NULL,
    embedding   vector(1536),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (document_id, chunk_index)
);

CREATE INDEX IF NOT EXISTS idx_document_chunks_document_id
    ON document_chunks(document_id);

-- IVFFlat index for approximate nearest-neighbour cosine search.
-- lists=100 is suitable for up to ~1M rows; tune for production.
CREATE INDEX IF NOT EXISTS idx_document_chunks_embedding
    ON document_chunks USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);
