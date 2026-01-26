-- Migration: ensure id primary key on metrics.metrics_file (safe corrective migration)
-- Adds an auto-generated BIGINT id column, sequence and primary key if missing.

ALTER TABLE metrics.metrics_file
    ADD COLUMN IF NOT EXISTS id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'metrics_file_id_seq') THEN
        CREATE SEQUENCE metrics.metrics_file_id_seq OWNED BY metrics.metrics_file.id;
    END IF;

    -- Populate id for existing rows where null
    UPDATE metrics.metrics_file
    SET id = nextval('metrics.metrics_file_id_seq')
    WHERE id IS NULL;

    -- Ensure sequence is advanced to max(id)
    PERFORM setval('metrics.metrics_file_id_seq', COALESCE((SELECT MAX(id) FROM metrics.metrics_file), 1));
END $$;

-- Ensure id column uses the sequence for future inserts
ALTER TABLE metrics.metrics_file
    ALTER COLUMN id SET DEFAULT nextval('metrics.metrics_file_id_seq');

-- Replace primary key with id (drop old PK if present)
ALTER TABLE metrics.metrics_file
    DROP CONSTRAINT IF EXISTS metrics_file_pkey;

ALTER TABLE metrics.metrics_file
    ADD CONSTRAINT metrics_file_pkey PRIMARY KEY (id);

-- Make filename NOT NULL (do NOT enforce uniqueness)
ALTER TABLE metrics.metrics_file
    ALTER COLUMN filename SET NOT NULL;
