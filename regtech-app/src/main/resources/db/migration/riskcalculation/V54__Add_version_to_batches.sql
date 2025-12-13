-- V54__Add_version_to_batches.sql
-- Add version column for optimistic locking to batches table
-- Safe to run multiple times - checks if column exists first

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'riskcalculation'
        AND table_name = 'batches'
        AND column_name = 'version'
    ) THEN
        ALTER TABLE riskcalculation.batches
        ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

        -- Add comment to the column
        COMMENT ON COLUMN riskcalculation.batches.version IS 'Version field for optimistic locking';
    END IF;
END $$;