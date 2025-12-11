-- V54__Add_version_to_batches.sql
-- Add version column for optimistic locking to batches table

ALTER TABLE riskcalculation.batches
ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Add comment to the column
COMMENT ON COLUMN riskcalculation.batches.version IS 'Version field for optimistic locking';

-- Create index for version if needed (though not typically indexed for version fields)
-- CREATE INDEX idx_batches_version ON riskcalculation.batches(version);