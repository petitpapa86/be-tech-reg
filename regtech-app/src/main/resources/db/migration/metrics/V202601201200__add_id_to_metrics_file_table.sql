-- Migration: add numeric id primary key to metrics.metrics_file
-- Adds an auto-generated BIGSERIAL id column, populate existing rows, and switch primary key.
-- This migration is safe to run against existing DBs with existing data.

ALTER TABLE metrics.metrics_file
    ADD COLUMN IF NOT EXISTS id BIGINT;

-- If id values are missing, set them using a sequence
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

-- Drop the old primary key on filename (if it exists) and set new primary key on id
ALTER TABLE metrics.metrics_file
    DROP CONSTRAINT IF EXISTS metrics_file_pkey;

ALTER TABLE metrics.metrics_file
    ADD CONSTRAINT metrics_file_pkey PRIMARY KEY (id);

-- Make filename NOT NULL and unique
ALTER TABLE metrics.metrics_file
    ALTER COLUMN filename SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS metrics_file_filename_uq ON metrics.metrics_file (filename);

