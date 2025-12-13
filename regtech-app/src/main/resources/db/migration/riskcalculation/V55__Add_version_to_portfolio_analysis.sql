-- V55__Add_version_to_portfolio_analysis.sql
-- Add version column for optimistic locking to portfolio_analysis table
-- Safe to run multiple times - checks if column exists first

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'riskcalculation'
        AND table_name = 'portfolio_analysis'
        AND column_name = 'version'
    ) THEN
        ALTER TABLE riskcalculation.portfolio_analysis
        ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

        -- Add comment to the column
        COMMENT ON COLUMN riskcalculation.portfolio_analysis.version IS 'Version field for optimistic locking';
    END IF;
END $$;