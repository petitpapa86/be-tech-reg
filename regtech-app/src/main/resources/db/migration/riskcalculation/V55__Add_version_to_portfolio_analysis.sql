-- V55__Add_version_to_portfolio_analysis.sql
-- Add version column for optimistic locking to portfolio_analysis table

ALTER TABLE riskcalculation.portfolio_analysis
ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Add comment to the column
COMMENT ON COLUMN riskcalculation.portfolio_analysis.version IS 'Version field for optimistic locking';