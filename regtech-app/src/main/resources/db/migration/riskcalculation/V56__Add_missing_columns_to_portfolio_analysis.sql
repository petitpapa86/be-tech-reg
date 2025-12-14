-- V56__Add_missing_columns_to_portfolio_analysis.sql
-- Add missing columns to portfolio_analysis table for processing state tracking

ALTER TABLE riskcalculation.portfolio_analysis
ADD COLUMN IF NOT EXISTS processing_state VARCHAR(20),
ADD COLUMN IF NOT EXISTS total_exposures INTEGER,
ADD COLUMN IF NOT EXISTS processed_exposures INTEGER,
ADD COLUMN IF NOT EXISTS started_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS last_updated_at TIMESTAMP;

COMMENT ON COLUMN riskcalculation.portfolio_analysis.processing_state IS 'Current processing state of the portfolio analysis';
COMMENT ON COLUMN riskcalculation.portfolio_analysis.total_exposures IS 'Total number of exposures to process';
COMMENT ON COLUMN riskcalculation.portfolio_analysis.processed_exposures IS 'Number of exposures already processed';
COMMENT ON COLUMN riskcalculation.portfolio_analysis.started_at IS 'Timestamp when processing started';
COMMENT ON COLUMN riskcalculation.portfolio_analysis.last_updated_at IS 'Timestamp of last update during processing';