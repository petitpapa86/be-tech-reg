-- Manual fix for missing report_metadata_failures table
-- Run this script in your local PostgreSQL database (regtech)

-- Create the report_metadata_failures table
CREATE TABLE IF NOT EXISTS reportgeneration.report_metadata_failures (
    id UUID PRIMARY KEY,
    batch_id VARCHAR(255) NOT NULL,
    html_s3_uri TEXT,
    xbrl_s3_uri TEXT,
    failed_at TIMESTAMP NOT NULL,
    retry_count INT DEFAULT 0,
    last_retry_at TIMESTAMP
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_report_metadata_failures_batch_id ON reportgeneration.report_metadata_failures(batch_id);
CREATE INDEX IF NOT EXISTS idx_report_metadata_failures_failed_at ON reportgeneration.report_metadata_failures(failed_at);

-- Add comments
COMMENT ON TABLE reportgeneration.report_metadata_failures IS 'Fallback table for storing metadata when database insert fails after S3 upload';
COMMENT ON COLUMN reportgeneration.report_metadata_failures.id IS 'Unique identifier for the failed metadata record';
COMMENT ON COLUMN reportgeneration.report_metadata_failures.batch_id IS 'Reference to the batch that failed';
COMMENT ON COLUMN reportgeneration.report_metadata_failures.html_s3_uri IS 'S3 URI for the HTML report that was successfully uploaded';
COMMENT ON COLUMN reportgeneration.report_metadata_failures.xbrl_s3_uri IS 'S3 URI for the XBRL report that was successfully uploaded';
COMMENT ON COLUMN reportgeneration.report_metadata_failures.failed_at IS 'Timestamp when the database insert failed';
COMMENT ON COLUMN reportgeneration.report_metadata_failures.retry_count IS 'Number of retry attempts made';
COMMENT ON COLUMN reportgeneration.report_metadata_failures.last_retry_at IS 'Timestamp of the last retry attempt';

-- Update Flyway schema history to mark V60 as successful (if not already)
-- This prevents Flyway from trying to re-run the migration
DO $$
DECLARE
    next_rank INTEGER;
BEGIN
    -- Check if V60 migration already exists
    IF NOT EXISTS (
        SELECT 1 FROM flyway_schema_history 
        WHERE version = '60' AND script = 'V60__create_report_generation_tables.sql'
    ) THEN
        -- Get the next available rank
        SELECT COALESCE(MAX(installed_rank), 0) + 1 INTO next_rank FROM flyway_schema_history;
        
        -- Insert the migration record
        INSERT INTO flyway_schema_history (
            installed_rank, version, description, type, script, 
            checksum, installed_by, installed_on, execution_time, success
        ) VALUES (
            next_rank,
            '60',
            'create report generation tables',
            'SQL',
            'V60__create_report_generation_tables.sql',
            -1234567890,
            'postgres',
            NOW(),
            0,
            true
        );
        
        RAISE NOTICE 'Flyway migration V60 added to schema history with rank %', next_rank;
    ELSE
        RAISE NOTICE 'Flyway migration V60 already exists in schema history';
    END IF;
END $$;
